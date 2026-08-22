package br.com.nutau.services;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.models.entities.CartaoCredito;
import br.com.nutau.models.entities.Usuario;
import br.com.nutau.models.enums.CategoriaCartao;
import br.com.nutau.models.enums.StatusCartao;
import br.com.nutau.repositories.CartaoCreditoRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissao de cartoes e politica de limite.
 *
 * <p>Os percentuais e tetos vivem em {@link CategoriaCartao}, nao aqui: a regra de quanto
 * cada faixa concede pertence a propria faixa. Este service orquestra - descobre a
 * categoria do cliente, pede o calculo a ela e aplica o resultado.
 */
@Slf4j
@Service
public class CartaoService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CartaoCreditoRepository cartaoRepository;
    private final BigDecimal limiteMinimo;
    private final int validadeAnos;

    public CartaoService(
            CartaoCreditoRepository cartaoRepository,
            @Value("${nutau.cartao.limite-minimo}") BigDecimal limiteMinimo,
            @Value("${nutau.cartao.validade-anos}") int validadeAnos) {
        this.cartaoRepository = cartaoRepository;
        this.limiteMinimo = limiteMinimo;
        this.validadeAnos = validadeAnos;
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CartaoCredito buscarDoUsuario(UUID usuarioId) {
        return cartaoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NutauException(ErrorCode.CARTAO_NAO_ENCONTRADO));
    }

    @Transactional(readOnly = true)
    public Optional<CartaoCredito> procurarDoUsuario(UUID usuarioId) {
        return cartaoRepository.findByUsuarioId(usuarioId);
    }

    // ------------------------------------------------------------------
    // Politica de limite
    // ------------------------------------------------------------------

    /**
     * Categoria a que a renda do cliente da direito.
     *
     * <p>Hoje depende so da renda declarada. Na Fase 2 o score dos bureaus entrara na
     * decisao, e este metodo e o unico ponto que precisara mudar.
     */
    public CategoriaCartao definirCategoria(Usuario usuario) {
        return CategoriaCartao.paraRenda(usuario.getRendaMensal());
    }

    /** Limite do primeiro cartao, pelo percentual de emissao da categoria. */
    public BigDecimal calcularLimiteInicial(Usuario usuario) {
        BigDecimal calculado =
                definirCategoria(usuario).calcularLimiteEmissao(usuario.getRendaMensal());

        // O piso evita emitir cartao com limite irrisorio para rendas muito baixas.
        return calculado.max(limiteMinimo);
    }

    /**
     * Teto de limite do cartao - o alvo maximo de qualquer aumento.
     *
     * <p>Usa a categoria <b>do cartao</b>, nao a que a renda atual daria. A distincao
     * importa: um cliente com cartao INICIAL cuja renda ja alcanca AMBAR nao passa a ter
     * o teto de AMBAR automaticamente. Subir de faixa e uma promocao explicita, via
     * {@link #avaliarPromocao}, e nao um efeito colateral de consultar o limite.
     */
    public BigDecimal calcularTetoAumento(CartaoCredito cartao, Usuario usuario) {
        return cartao.getCategoria().calcularLimiteMaximo(usuario.getRendaMensal());
    }

    /**
     * Quanto conceder num pedido de aumento: o menor entre o pedido e o teto da categoria.
     *
     * <p>Quando o teto corta o pedido, o cliente ainda recebe credito - so que menos do
     * que queria. E o caso que produz {@code APROVADO_PARCIAL}.
     */
    public BigDecimal calcularAumentoConcedido(
            CartaoCredito cartao, Usuario usuario, BigDecimal valorSolicitado) {
        return valorSolicitado.min(calcularTetoAumento(cartao, usuario));
    }

    // ------------------------------------------------------------------
    // Efeitos da aprovacao
    // ------------------------------------------------------------------

    /**
     * Cria o cartao de um pedido de emissao aprovado, ja na categoria devida.
     *
     * <p>Chamado quando a analise nos bureaus termina em aprovacao. A restricao unica em
     * cartoes.usuario_id e a garantia final contra emissao duplicada, mesmo que a mesma
     * mensagem seja processada duas vezes.
     */
    @Transactional
    public CartaoCredito emitirPara(Usuario usuario, BigDecimal limite) {
        if (cartaoRepository.existsByUsuarioId(usuario.getId())) {
            throw new NutauException(ErrorCode.CARTAO_JA_EMITIDO);
        }

        CategoriaCartao categoria = definirCategoria(usuario);

        CartaoCredito cartao = CartaoCredito.builder()
                .usuario(usuario)
                .categoria(categoria)
                .ultimosDigitos(gerarUltimosDigitos())
                .limite(limite)
                .status(StatusCartao.ATIVO)
                .validadeAte(LocalDate.now().plusYears(validadeAnos))
                .build();

        CartaoCredito salvo = cartaoRepository.save(cartao);
        log.info("Cartao emitido: id={} usuarioId={} categoria={} limite={}",
                salvo.getId(), usuario.getId(), categoria, limite);
        return salvo;
    }

    /** Aplica o novo limite a um cartao existente. */
    @Transactional
    public CartaoCredito elevarLimite(CartaoCredito cartao, BigDecimal novoLimite) {
        cartao.elevarLimitePara(novoLimite);
        CartaoCredito salvo = cartaoRepository.save(cartao);
        log.info("Limite elevado: cartaoId={} novoLimite={}", salvo.getId(), novoLimite);
        return salvo;
    }

    /**
     * Promove o cartao caso a renda atual do cliente ja alcance uma categoria superior.
     *
     * <p>Devolve a categoria vigente ao final, tenha havido promocao ou nao.
     */
    @Transactional
    public CategoriaCartao avaliarPromocao(Usuario usuario, CartaoCredito cartao) {
        CategoriaCartao devida = definirCategoria(usuario);

        if (!devida.superiorA(cartao.getCategoria())) {
            return cartao.getCategoria();
        }

        CategoriaCartao anterior = cartao.getCategoria();
        cartao.promoverPara(devida);
        cartaoRepository.save(cartao);

        log.info("Cartao promovido: id={} de={} para={}", cartao.getId(), anterior, devida);
        return devida;
    }

    // ------------------------------------------------------------------
    // Validacoes de pre-condicao
    // ------------------------------------------------------------------

    /** Garante que o cliente pode abrir um pedido de emissao. */
    public void validarPodeEmitir(UUID usuarioId) {
        if (cartaoRepository.existsByUsuarioId(usuarioId)) {
            throw new NutauException(ErrorCode.CARTAO_JA_EMITIDO);
        }
    }

    /**
     * Garante que o pedido de aumento faz sentido antes de gastar uma analise com ele.
     *
     * <p>Rejeitar aqui e barato; rejeitar depois custaria duas consultas a bureaus e
     * cinco segundos de processamento para chegar ao mesmo "nao".
     */
    public CartaoCredito validarPodeAumentar(Usuario usuario, BigDecimal valorSolicitado) {
        CartaoCredito cartao = buscarDoUsuario(usuario.getId());

        if (!cartao.getStatus().permiteAumentoLimite()) {
            throw new NutauException(ErrorCode.CARTAO_INATIVO);
        }
        if (valorSolicitado.compareTo(cartao.getLimite()) <= 0) {
            throw new NutauException(
                    ErrorCode.VALOR_ABAIXO_DO_LIMITE_ATUAL,
                    "Pedido de " + valorSolicitado + " nao supera o limite atual de "
                            + cartao.getLimite());
        }
        if (calcularTetoAumento(cartao, usuario).compareTo(cartao.getLimite()) <= 0) {
            throw new NutauException(
                    ErrorCode.LIMITE_MAXIMO_ATINGIDO,
                    "Teto da categoria " + cartao.getCategoria() + " ja alcancado: "
                            + cartao.getLimite());
        }
        return cartao;
    }

    /** Quatro digitos aleatorios apenas para exibicao. Nao ha PAN neste sistema. */
    private String gerarUltimosDigitos() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }
}
