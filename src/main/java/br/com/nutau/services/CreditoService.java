package br.com.nutau.services;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.models.entities.CartaoCredito;
import br.com.nutau.models.entities.SolicitacaoCredito;
import br.com.nutau.models.entities.Usuario;
import br.com.nutau.models.enums.StatusSolicitacao;
import br.com.nutau.models.enums.TipoSolicitacao;
import br.com.nutau.repositories.SolicitacaoCreditoRepository;
import br.com.nutau.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abertura e acompanhamento de solicitacoes de credito (US03).
 *
 * <p>Os dois tipos de pedido - emissao do primeiro cartao e aumento de limite - passam
 * pela mesma analise nos bureaus. O que os separa sao as pre-condicoes verificadas aqui
 * e o efeito aplicado depois da aprovacao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditoService {

    private final SolicitacaoCreditoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CartaoService cartaoService;

    /**
     * Solicita a emissao do primeiro cartao.
     *
     * <p>Nao ha valor a informar: o cliente ainda nao tem referencia nenhuma de credito, e
     * quem arbitra o limite inicial e o banco, a partir da renda declarada no cadastro. O
     * calculo so acontece depois da aprovacao - pedir credito nao garante recebe-lo, e
     * gravar um limite antes da analise seria prometer o que ainda nao foi decidido.
     */
    @Transactional
    public SolicitacaoCredito solicitarEmissao(UUID usuarioId) {
        Usuario usuario = carregarUsuario(usuarioId);

        garantirSemAnaliseEmAndamento(usuarioId);
        cartaoService.validarPodeEmitir(usuarioId);

        SolicitacaoCredito solicitacao = SolicitacaoCredito.builder()
                .usuario(usuario)
                .tipo(TipoSolicitacao.EMISSAO_CARTAO)
                .status(StatusSolicitacao.EM_ANALISE)
                .build();

        return registrar(solicitacao);
    }

    /**
     * Solicita aumento de limite sobre o cartao existente.
     *
     * <p>Aqui o valor faz sentido: o cliente tem um limite atual e pede mais que ele. O
     * cartao ja fica vinculado a solicitacao desde o inicio, ao contrario da emissao,
     * onde o cartao so passa a existir se a analise aprovar.
     */
    @Transactional
    public SolicitacaoCredito solicitarAumento(UUID usuarioId, BigDecimal valorSolicitado) {
        Usuario usuario = carregarUsuario(usuarioId);

        garantirSemAnaliseEmAndamento(usuarioId);
        CartaoCredito cartao = cartaoService.validarPodeAumentar(usuario, valorSolicitado);

        SolicitacaoCredito solicitacao = SolicitacaoCredito.builder()
                .usuario(usuario)
                .cartao(cartao)
                .tipo(TipoSolicitacao.AUMENTO_LIMITE)
                .valorSolicitado(valorSolicitado)
                .status(StatusSolicitacao.EM_ANALISE)
                .build();

        return registrar(solicitacao);
    }

    /**
     * Grava a solicitacao e devolve na hora, sem esperar a analise.
     *
     * <p>A analise leva segundos: sao duas consultas a bureaus externos, cada uma sujeita
     * a lentidao e falha. Segurar a conexao HTTP durante esse tempo custaria uma thread do
     * servidor por cliente esperando e faria a experiencia do usuario depender da
     * disponibilidade de um terceiro. Por isso o endpoint apenas aceita o pedido - HTTP
     * 202 - e devolve o identificador para acompanhamento.
     */
    private SolicitacaoCredito registrar(SolicitacaoCredito solicitacao) {
        SolicitacaoCredito salva = solicitacaoRepository.save(solicitacao);

        log.info("Solicitacao registrada: id={} tipo={} usuarioId={} valor={}",
                salva.getId(),
                salva.getTipo(),
                salva.getUsuario().getId(),
                salva.getValorSolicitado());

        // ------------------------------------------------------------------
        // FASE 2 - MENSAGERIA
        // Aqui entra a publicacao de CreditoSolicitadoEvent no Kafka (US03/US04).
        // Deixado explicito de proposito: e o ponto exato onde o fluxo sincrono
        // termina e o assincrono comeca.
        // ------------------------------------------------------------------

        return salva;
    }

    /** Consulta uma solicitacao do proprio cliente. */
    @Transactional(readOnly = true)
    public SolicitacaoCredito buscarDoUsuario(UUID solicitacaoId, UUID usuarioId) {
        return solicitacaoRepository.findByIdAndUsuarioId(solicitacaoId, usuarioId)
                .orElseThrow(() -> new NutauException(ErrorCode.SOLICITACAO_NAO_ENCONTRADA));
    }

    /** Historico de solicitacoes do cliente, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<SolicitacaoCredito> listarDoUsuario(UUID usuarioId) {
        return solicitacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
    }

    private Usuario carregarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NutauException(ErrorCode.USUARIO_NAO_ENCONTRADO));
    }

    /**
     * Bloqueia um segundo pedido enquanto o primeiro nao termina.
     *
     * <p>Sem esta regra, cinco POSTs seguidos abrem cinco analises paralelas do mesmo
     * cliente - e, na emissao, cinco tentativas de criar cartao. A constraint unica em
     * cartoes.usuario_id ainda barraria a duplicata no banco, mas so depois de gastar
     * dez consultas a bureaus para descobrir isso.
     */
    private void garantirSemAnaliseEmAndamento(UUID usuarioId) {
        if (solicitacaoRepository.existsByUsuarioIdAndStatus(
                usuarioId, StatusSolicitacao.EM_ANALISE)) {
            throw new NutauException(ErrorCode.SOLICITACAO_EM_ANDAMENTO);
        }
    }
}
