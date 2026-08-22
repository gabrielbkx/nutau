package br.com.nutau.models.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("CategoriaCartao")
class CategoriaCartaoTest {

    @Nested
    @DisplayName("paraRenda")
    class ParaRenda {

        @ParameterizedTest(name = "renda {0} -> {1}")
        @CsvSource({
            "0.00,      INICIAL",
            "1500.00,   INICIAL",
            "2999.99,   INICIAL",
            "3000.00,   AMBAR",     // exatamente no piso da faixa
            "6999.99,   AMBAR",
            "7000.00,   VIOLETA",
            "14999.99,  VIOLETA",
            "15000.00,  INFINITO",
            "80000.00,  INFINITO"
        })
        @DisplayName("deve escolher a maior categoria que a renda alcanca")
        void deveEscolherMaiorCategoriaAlcancada(String renda, CategoriaCartao esperada) {
            assertThat(CategoriaCartao.paraRenda(new BigDecimal(renda))).isEqualTo(esperada);
        }

        @Test
        @DisplayName("deve recusar renda nula em vez de assumir a categoria de entrada")
        void deveRecusarRendaNula() {
            // Assumir INICIAL para renda nula esconderia um bug de mapeamento -
            // exatamente o que ja aconteceu com renda_mensal no cadastro.
            assertThatThrownBy(() -> CategoriaCartao.paraRenda(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("calculo de limites")
    class Limites {

        @ParameterizedTest(name = "renda {0} -> emissao {1}, teto {2}")
        @CsvSource({
            // INICIAL: 30% na emissao, ate 50%, teto absoluto de 3.000
            "2000.00,    600.00,   1000.00",
            // AMBAR: 35% na emissao, ate 70%, teto absoluto de 10.000
            "4000.00,   1400.00,   2800.00",
            // VIOLETA: 40% na emissao, ate 100%, teto absoluto de 30.000
            "10000.00,  4000.00,  10000.00",
            // INFINITO: 50% na emissao, ate 150%, teto absoluto de 100.000
            "20000.00, 10000.00,  30000.00"
        })
        @DisplayName("deve aplicar os percentuais da categoria correspondente a renda")
        void deveAplicarPercentuaisDaCategoria(
                String renda, String emissaoEsperada, String tetoEsperado) {

            BigDecimal valor = new BigDecimal(renda);
            CategoriaCartao categoria = CategoriaCartao.paraRenda(valor);

            assertThat(categoria.calcularLimiteEmissao(valor))
                    .isEqualByComparingTo(new BigDecimal(emissaoEsperada));
            assertThat(categoria.calcularLimiteMaximo(valor))
                    .isEqualByComparingTo(new BigDecimal(tetoEsperado));
        }

        @Test
        @DisplayName("o teto absoluto deve cortar rendas muito altas")
        void tetoAbsolutoDeveCortar() {
            // 500.000 * 150% = 750.000, mas INFINITO nao passa de 100.000.
            BigDecimal renda = new BigDecimal("500000.00");

            assertThat(CategoriaCartao.INFINITO.calcularLimiteMaximo(renda))
                    .isEqualByComparingTo(new BigDecimal("100000.00"));
        }

        @ParameterizedTest
        @EnumSource(CategoriaCartao.class)
        @DisplayName("o limite de emissao nunca deve superar o teto da propria categoria")
        void emissaoNuncaSuperaTeto(CategoriaCartao categoria) {
            BigDecimal rendaAbsurda = new BigDecimal("1000000.00");

            assertThat(categoria.calcularLimiteEmissao(rendaAbsurda))
                    .isLessThanOrEqualTo(categoria.getTetoAbsoluto());
        }

        @ParameterizedTest
        @EnumSource(CategoriaCartao.class)
        @DisplayName("o percentual maximo deve superar o de emissao, senao nao ha aumento possivel")
        void percentualMaximoDeveSuperarEmissao(CategoriaCartao categoria) {
            // Se os dois fossem iguais, o cartao nasceria ja no teto e todo pedido de
            // aumento seria recusado com LIMITE_MAXIMO_ATINGIDO.
            assertThat(categoria.getPercentualMaximo())
                    .isGreaterThan(categoria.getPercentualEmissao());
        }
    }

    @Nested
    @DisplayName("consistencia do catalogo")
    class Consistencia {

        @Test
        @DisplayName("INICIAL deve aceitar qualquer renda, para que paraRenda sempre responda")
        void inicialDeveAceitarQualquerRenda() {
            assertThat(CategoriaCartao.INICIAL.getRendaMinima())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("as faixas devem ser estritamente crescentes em renda e teto")
        void faixasDevemSerCrescentes() {
            CategoriaCartao[] ordem = {
                CategoriaCartao.INICIAL,
                CategoriaCartao.AMBAR,
                CategoriaCartao.VIOLETA,
                CategoriaCartao.INFINITO
            };

            for (int i = 1; i < ordem.length; i++) {
                assertThat(ordem[i].getRendaMinima())
                        .as("renda minima de %s", ordem[i])
                        .isGreaterThan(ordem[i - 1].getRendaMinima());
                assertThat(ordem[i].getTetoAbsoluto())
                        .as("teto de %s", ordem[i])
                        .isGreaterThan(ordem[i - 1].getTetoAbsoluto());
                assertThat(ordem[i].superiorA(ordem[i - 1])).isTrue();
            }
        }

        @Test
        @DisplayName("superiorA deve ser falso para a propria categoria")
        void superiorANaoDeveIncluirIgualdade() {
            assertThat(CategoriaCartao.VIOLETA.superiorA(CategoriaCartao.VIOLETA)).isFalse();
        }

        @Test
        @DisplayName("cashback deve ser exibido em pontos percentuais")
        void cashbackDeveSerExibidoEmPontosPercentuais() {
            // 0.010 armazenado -> "1" exibido. O cliente le "1% de cashback".
            assertThat(CategoriaCartao.VIOLETA.getCashbackPercentual())
                    .isEqualByComparingTo(BigDecimal.ONE);
            assertThat(CategoriaCartao.INICIAL.isIsentaDeAnuidade()).isTrue();
            assertThat(CategoriaCartao.AMBAR.isIsentaDeAnuidade()).isFalse();
        }
    }
}
