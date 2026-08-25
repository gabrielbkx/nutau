package br.com.nutau.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nutau.models.dtos.EnderecoRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CEP: helper e validacao declarativa")
class CepHelperTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void abrirValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    /** Erros de validacao apenas do campo cep. */
    private static long violacoesDeCep(String cep) {
        return validator.validate(new EnderecoRequest(cep, "1500", null)).stream()
                .filter(v -> v.getPropertyPath().toString().equals("cep"))
                .count();
    }

    @Nested
    @DisplayName("validacao declarativa via @Pattern")
    class Validacao {

        @ParameterizedTest(name = "aceita {0}")
        @ValueSource(strings = {"01001000", "01001-000", "99999999", "99999-999"})
        void cep_deveSerAceito_quandoTem8DigitosComOuSemHifen(String cep) {
            assertThat(violacoesDeCep(cep)).isZero();
        }

        @ParameterizedTest(name = "recusa {0}")
        @ValueSource(strings = {
            "0100100",      // 7 digitos
            "010010000",    // 9 digitos
            "01001-00",     // curto demais depois do hifen
            "0100-1000",    // hifen na posicao errada
            "abcde-fgh",    // letras
            "01001 000",    // espaco no lugar do hifen
        })
        void cep_deveSerRecusado_quandoForaDoFormato(String cep) {
            assertThat(violacoesDeCep(cep)).isPositive();
        }

        @Test
        @DisplayName("cep em branco e recusado pelo @NotBlank")
        void cep_deveSerRecusado_quandoEmBranco() {
            assertThat(violacoesDeCep("   ")).isPositive();
        }

        @Test
        @DisplayName("complemento e opcional")
        void complemento_devePassar_quandoNulo() {
            assertThat(validator.validate(new EnderecoRequest("01001-000", "1500", null)))
                    .isEmpty();
        }

        @Test
        @DisplayName("numero em branco e recusado: o ViaCEP nao descobre o numero da casa")
        void numero_deveSerRecusado_quandoEmBranco() {
            assertThat(validator.validate(new EnderecoRequest("01001-000", "  ", null)))
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("normalizar")
    class Normalizar {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "01001-000, 01001000",
            "01001000, 01001000",
            "01.001-000, 01001000",
        })
        void normalizar_deveDevolverSomenteDigitos(String entrada, String esperado) {
            assertThat(CepHelper.normalizar(entrada)).isEqualTo(esperado);
        }

        @Test
        void normalizar_deveDevolverNulo_quandoEntradaNula() {
            assertThat(CepHelper.normalizar(null)).isNull();
        }
    }

    @Nested
    @DisplayName("formatar")
    class Formatar {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "01001000, 01001-000",
            "01001-000, 01001-000",
        })
        void formatar_deveAplicarHifen_quandoTem8Digitos(String entrada, String esperado) {
            assertThat(CepHelper.formatar(entrada)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "mantem {0} intacto")
        @ValueSource(strings = {"123", "0100100012345"})
        void formatar_deveDevolverEntrada_quandoNaoTem8Digitos(String entrada) {
            // Formatar um valor fora do padrao produziria um CEP inventado. Melhor
            // devolver o que veio e deixar a inconsistencia visivel.
            assertThat(CepHelper.formatar(entrada)).isEqualTo(entrada);
        }

        @Test
        void formatar_deveDevolverNulo_quandoEntradaNula() {
            assertThat(CepHelper.formatar(null)).isNull();
        }
    }

    @Test
    @DisplayName("a classe utilitaria nao deve poder ser instanciada")
    void construtor_deveLancar_quandoInvocadoPorReflexao() throws Exception {
        Constructor<CepHelper> construtor = CepHelper.class.getDeclaredConstructor();
        construtor.setAccessible(true);

        assertThatThrownBy(construtor::newInstance)
                .hasRootCauseInstanceOf(AssertionError.class);
    }
}
