package br.com.nutau.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nutau.models.dtos.CadastroRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CPF: helper e validacao declarativa")
class CpfHelperTest {

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

    private static CadastroRequest cadastroCom(String cpf) {
        return new CadastroRequest(
                "Cliente Teste", "cliente@exemplo.com", cpf, "senhaSegura123",
                new BigDecimal("4000.00"));
    }

    /** Erros de validacao apenas do campo cpf. */
    private static long violacoesDeCpf(String cpf) {
        return validator.validate(cadastroCom(cpf)).stream()
                .filter(v -> v.getPropertyPath().toString().equals("cpf"))
                .count();
    }

    @Nested
    @DisplayName("validacao declarativa via @Pattern")
    class Validacao {

        @ParameterizedTest(name = "aceita {0}")
        @ValueSource(strings = {
            "12345678901",
            "123.456.789-01",
            "123456789-01",
            "123.456.78901",
            "00000000100",
            "52998224745"
        })
        @DisplayName("deve aceitar 11 digitos, com ou sem mascara")
        void deveAceitarFormatoValido(String cpf) {
            assertThat(violacoesDeCpf(cpf)).isZero();
        }

        @ParameterizedTest(name = "rejeita {0}")
        @ValueSource(strings = {
            "1234567890",       // 10 digitos
            "123456789012",     // 12 digitos
            "abcdefghijk",      // sem digito
            "123.456.789-0a",   // letra no fim
            "11111111111",      // sequencia repetida, sem mascara
            "111.111.111-11",   // sequencia repetida, com mascara
            "00000000000"
        })
        @DisplayName("deve rejeitar formato invalido e sequencia repetida")
        void deveRejeitarFormatoInvalido(String cpf) {
            assertThat(violacoesDeCpf(cpf)).isPositive();
        }

        @Test
        @DisplayName("CPF ausente deve acusar apenas o @NotBlank, sem duplicar mensagem")
        void ausenteDeveAcusarApenasNotBlank() {
            // @Pattern nao roda em valor nulo: cada anotacao cuida de uma regra so,
            // senao o cliente recebe duas mensagens para o mesmo problema.
            assertThat(violacoesDeCpf(null)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("normalizar")
    class Normalizar {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "123.456.789-01, 12345678901",
            "12345678901,    12345678901",
            "123 456 789-01, 12345678901"
        })
        @DisplayName("deve remover a mascara quando ela existe")
        void deveRemoverMascara(String entrada, String esperado) {
            assertThat(CpfHelper.normalizar(entrada)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("deve devolver nulo quando a entrada e nula")
        void deveDevolverNuloQuandoEntradaNula() {
            assertThat(CpfHelper.normalizar(null)).isNull();
        }
    }

    @Nested
    @DisplayName("mascarar")
    class Mascarar {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "52998224745,     ***.982.247-**",
            "529.982.247-45,  ***.982.247-**"
        })
        @DisplayName("deve revelar apenas os digitos do meio")
        void deveRevelarApenasOMeio(String entrada, String esperado) {
            assertThat(CpfHelper.mascarar(entrada)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("deve devolver a entrada intacta quando nao ha 11 digitos")
        void deveDevolverIntactoQuandoTamanhoInesperado() {
            assertThat(CpfHelper.mascarar("123")).isEqualTo("123");
            assertThat(CpfHelper.mascarar(null)).isNull();
        }
    }

    @Test
    @DisplayName("a classe utilitaria nao deve ser instanciavel")
    void naoDeveSerInstanciavel() throws Exception {
        Constructor<CpfHelper> construtor = CpfHelper.class.getDeclaredConstructor();
        construtor.setAccessible(true);

        assertThatThrownBy(construtor::newInstance)
                .hasRootCauseInstanceOf(AssertionError.class);
    }
}
