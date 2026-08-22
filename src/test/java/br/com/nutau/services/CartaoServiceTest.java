package br.com.nutau.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.models.entities.CartaoCredito;
import br.com.nutau.models.entities.Usuario;
import br.com.nutau.models.enums.CategoriaCartao;
import br.com.nutau.models.enums.StatusCartao;
import br.com.nutau.repositories.CartaoCreditoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartaoService")
class CartaoServiceTest {

    @Mock
    private CartaoCreditoRepository cartaoRepository;

    private CartaoService service;
    private UUID usuarioId;

    @BeforeEach
    void preparar() {
        service = new CartaoService(
                cartaoRepository,
                new BigDecimal("100.00"), // piso do limite inicial
                5);
        usuarioId = UUID.randomUUID();
    }

    private Usuario usuarioComRenda(String renda) {
        return Usuario.builder()
                .id(usuarioId)
                .nome("Cliente Teste")
                .email("cliente@exemplo.com")
                .cpf("12345678945")
                .senha("$2a$12$hashfalso")
                .rendaMensal(new BigDecimal(renda))
                .build();
    }

    private CartaoCredito cartaoCom(String limite, StatusCartao status) {
        return cartaoCom(limite, status, CategoriaCartao.AMBAR);
    }

    private CartaoCredito cartaoCom(
            String limite, StatusCartao status, CategoriaCartao categoria) {
        return CartaoCredito.builder()
                .id(UUID.randomUUID())
                .usuario(usuarioComRenda("4000.00"))
                .categoria(categoria)
                .ultimosDigitos("4821")
                .limite(new BigDecimal(limite))
                .status(status)
                .validadeAte(LocalDate.now().plusYears(5))
                .build();
    }

    @Nested
    @DisplayName("calcularLimiteInicial")
    class LimiteInicial {

        @ParameterizedTest(name = "renda {0} -> limite {1}")
        @CsvSource({
            "1000.00,     300.00",  // INICIAL: 30%
            "2000.00,     600.00",  // INICIAL: 30%
            "4000.00,    1400.00",  // AMBAR: 35%
            "10000.00,   4000.00",  // VIOLETA: 40%
            "20000.00,  10000.00",  // INFINITO: 50%
            "300.00,      100.00"   // piso protege rendas muito baixas (30% = 90)
        })
        @DisplayName("deve aplicar o percentual da categoria respeitando teto e piso")
        void deveAplicarPercentualComTetoEPiso(String renda, String esperado) {
            BigDecimal limite = service.calcularLimiteInicial(usuarioComRenda(renda));

            assertThat(limite).isEqualByComparingTo(new BigDecimal(esperado));
        }

        @Test
        @DisplayName("deve arredondar para baixo quando o calculo gera fracao de centavo")
        void deveArredondarParaBaixo() {
            // Renda 1333.33 cai em INICIAL: 1333.33 * 0.30 = 399.999
            // -> concede 399.99, nunca 400.00. Na duvida sobre o centavo, concede a menos.
            BigDecimal limite = service.calcularLimiteInicial(usuarioComRenda("1333.33"));

            assertThat(limite).isEqualByComparingTo(new BigDecimal("399.99"));
        }

        @Test
        @DisplayName("deve definir a categoria a partir da renda declarada")
        void deveDefinirCategoriaPelaRenda() {
            assertThat(service.definirCategoria(usuarioComRenda("2000.00")))
                    .isEqualTo(CategoriaCartao.INICIAL);
            assertThat(service.definirCategoria(usuarioComRenda("4000.00")))
                    .isEqualTo(CategoriaCartao.AMBAR);
            assertThat(service.definirCategoria(usuarioComRenda("20000.00")))
                    .isEqualTo(CategoriaCartao.INFINITO);
        }
    }

    @Nested
    @DisplayName("calcularAumentoConcedido")
    class AumentoConcedido {

        @ParameterizedTest(name = "{0} renda {1}, pede {2} -> concede {3}")
        @CsvSource({
            "AMBAR,    4000.00,  8000.00,  2800.00",  // teto de 70% corta o pedido
            "AMBAR,    4000.00,  1500.00,  1500.00",  // dentro do teto: integral
            "AMBAR,    4000.00,  2800.00,  2800.00",  // exatamente no teto
            "VIOLETA, 10000.00,  4000.00,  4000.00",  // teto de 100% nao corta
            "INICIAL,  2000.00,  5000.00,  1000.00"   // teto de 50% corta
        })
        @DisplayName("deve conceder o menor entre o pedido e o teto da categoria do cartao")
        void deveConcederOMenorEntrePedidoETeto(
                CategoriaCartao categoria, String renda, String pedido, String esperado) {

            CartaoCredito cartao = cartaoCom("100.00", StatusCartao.ATIVO, categoria);

            BigDecimal concedido = service.calcularAumentoConcedido(
                    cartao, usuarioComRenda(renda), new BigDecimal(pedido));

            assertThat(concedido).isEqualByComparingTo(new BigDecimal(esperado));
        }

        @Test
        @DisplayName("deve usar a categoria do cartao, e nao a que a renda atual daria")
        void deveUsarCategoriaDoCartaoENaoDaRenda() {
            // Renda 10.000 daria VIOLETA (teto 100% = 10.000), mas o cartao ainda e
            // INICIAL (teto 50%, cortado pelo absoluto de 3.000). Subir de faixa e uma
            // promocao explicita, nao um efeito colateral de pedir aumento.
            CartaoCredito cartaoInicial =
                    cartaoCom("500.00", StatusCartao.ATIVO, CategoriaCartao.INICIAL);

            BigDecimal concedido = service.calcularAumentoConcedido(
                    cartaoInicial, usuarioComRenda("10000.00"), new BigDecimal("9000.00"));

            assertThat(concedido).isEqualByComparingTo(new BigDecimal("3000.00"));
        }
    }

    @Nested
    @DisplayName("validarPodeEmitir")
    class PodeEmitir {

        @Test
        @DisplayName("deve permitir quando o cliente ainda nao tem cartao")
        void devePermitir_quandoSemCartao() {
            when(cartaoRepository.existsByUsuarioId(usuarioId)).thenReturn(false);

            service.validarPodeEmitir(usuarioId);
        }

        @Test
        @DisplayName("deve recusar com CARTAO_JA_EMITIDO quando ja existe cartao")
        void deveRecusar_quandoJaTemCartao() {
            when(cartaoRepository.existsByUsuarioId(usuarioId)).thenReturn(true);

            assertThatThrownBy(() -> service.validarPodeEmitir(usuarioId))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CARTAO_JA_EMITIDO);
        }
    }

    @Nested
    @DisplayName("validarPodeAumentar")
    class PodeAumentar {

        @Test
        @DisplayName("deve devolver o cartao quando o pedido e legitimo")
        void deveDevolverCartao_quandoPedidoLegitimo() {
            CartaoCredito cartao = cartaoCom("1200.00", StatusCartao.ATIVO);
            when(cartaoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cartao));

            CartaoCredito resultado = service.validarPodeAumentar(
                    usuarioComRenda("4000.00"), new BigDecimal("1800.00"));

            assertThat(resultado).isSameAs(cartao);
        }

        @Test
        @DisplayName("deve recusar com CARTAO_NAO_ENCONTRADO quando nao ha cartao")
        void deveRecusar_quandoSemCartao() {
            when(cartaoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validarPodeAumentar(
                            usuarioComRenda("4000.00"), new BigDecimal("1800.00")))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CARTAO_NAO_ENCONTRADO);
        }

        @Test
        @DisplayName("deve recusar com CARTAO_INATIVO quando o cartao esta bloqueado")
        void deveRecusar_quandoCartaoBloqueado() {
            when(cartaoRepository.findByUsuarioId(usuarioId))
                    .thenReturn(Optional.of(cartaoCom("1200.00", StatusCartao.BLOQUEADO)));

            assertThatThrownBy(() -> service.validarPodeAumentar(
                            usuarioComRenda("4000.00"), new BigDecimal("1800.00")))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CARTAO_INATIVO);
        }

        @Test
        @DisplayName("deve recusar quando o valor pedido nao supera o limite atual")
        void deveRecusar_quandoValorNaoSuperaLimiteAtual() {
            when(cartaoRepository.findByUsuarioId(usuarioId))
                    .thenReturn(Optional.of(cartaoCom("1200.00", StatusCartao.ATIVO)));

            // Pedir 1200 tendo 1200 nao e um aumento - nao vale gastar analise com isso.
            assertThatThrownBy(() -> service.validarPodeAumentar(
                            usuarioComRenda("4000.00"), new BigDecimal("1200.00")))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALOR_ABAIXO_DO_LIMITE_ATUAL);
        }

        @Test
        @DisplayName("deve recusar com LIMITE_MAXIMO_ATINGIDO quando o cartao ja esta no teto")
        void deveRecusar_quandoJaNoTetoDaRenda() {
            // Renda 4000 cai em AMBAR, cujo teto e 70% = 2800. Com limite ja em 2800,
            // nao ha aumento possivel: recusar agora evita duas consultas a bureaus
            // para chegar ao mesmo "nao".
            when(cartaoRepository.findByUsuarioId(usuarioId))
                    .thenReturn(Optional.of(cartaoCom("2800.00", StatusCartao.ATIVO)));

            assertThatThrownBy(() -> service.validarPodeAumentar(
                            usuarioComRenda("4000.00"), new BigDecimal("5000.00")))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LIMITE_MAXIMO_ATINGIDO);
        }
    }

    @Nested
    @DisplayName("emitirPara")
    class Emitir {

        @Test
        @DisplayName("nao deve salvar nada quando o cliente ja possui cartao")
        void naoDeveSalvar_quandoJaExisteCartao() {
            when(cartaoRepository.existsByUsuarioId(usuarioId)).thenReturn(true);

            assertThatThrownBy(() -> service.emitirPara(
                            usuarioComRenda("4000.00"), new BigDecimal("1200.00")))
                    .isInstanceOf(NutauException.class);

            verify(cartaoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("CartaoCredito.elevarLimitePara")
    class ElevarLimite {

        @Test
        @DisplayName("deve recusar reducao de limite")
        void deveRecusarReducao() {
            CartaoCredito cartao = cartaoCom("1200.00", StatusCartao.ATIVO);

            // Um bug que diminua credito ja concedido e pior que um que nao o aumente.
            assertThatThrownBy(() -> cartao.elevarLimitePara(new BigDecimal("800.00")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(cartao.getLimite()).isEqualByComparingTo(new BigDecimal("1200.00"));
        }

        @Test
        @DisplayName("deve aplicar o novo limite quando ele e maior")
        void deveAplicarQuandoMaior() {
            CartaoCredito cartao = cartaoCom("1200.00", StatusCartao.ATIVO);

            cartao.elevarLimitePara(new BigDecimal("2000.00"));

            assertThat(cartao.getLimite()).isEqualByComparingTo(new BigDecimal("2000.00"));
        }
    }
}
