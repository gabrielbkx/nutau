package br.com.nutau.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.models.enums.ResultadoBureau;
import br.com.nutau.models.enums.TipoBureau;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("MockBureauService")
class MockBureauServiceTest {

    // Latencia zerada: a espera de 2,5s existe para simular rede em execucao real,
    // e nao tem valor nenhum dentro de um teste unitario.
    private final MockBureauService service = new MockBureauService(0L);

    @ParameterizedTest(name = "{0} + CPF ...{1} -> {2}")
    @CsvSource({
        // CPF terminado em 11: apenas o Serasa reprova, por score.
        "SERASA, 11, SCORE_BAIXO",
        "SPC,    11, APROVADO",
        // CPF terminado em 22: apenas o SPC reprova, por restricao ativa.
        "SPC,    22, NOME_SUJO",
        "SERASA, 22, APROVADO",
        // Qualquer outra terminacao: ambos aprovam.
        "SERASA, 45, APROVADO",
        "SPC,    45, APROVADO"
    })
    @DisplayName("consultar deve aplicar a regra do bureau quando o CPF tem sufixo conhecido")
    void consultar_deveAplicarRegraDoBureau_quandoSufixoConhecido(
            TipoBureau bureau, String sufixo, ResultadoBureau esperado) {

        String cpf = "123456789" + sufixo;

        assertThat(service.consultar(bureau, cpf)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @EnumSource(TipoBureau.class)
    @DisplayName("consultar deve lancar BUREAU_INDISPONIVEL quando o CPF termina em 00")
    void consultar_deveLancarBureauIndisponivel_quandoCpfTerminaEm00(TipoBureau bureau) {
        // Este e o cenario que exercita retentativa e DLQ no RabbitMQ:
        // falha tecnica, nao negativa de credito.
        assertThatThrownBy(() -> service.consultar(bureau, "12345678900"))
                .isInstanceOf(NutauException.class)
                .hasMessageContaining(bureau.name())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        NutauException.class))
                .satisfies(e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BUREAU_INDISPONIVEL);
                    // A falha e transitoria: o consumidor do RabbitMQ deve retentar
                    // antes de mandar a mensagem para a DLQ.
                    assertThat(e.isRetentavel()).isTrue();
                });
    }

    @Test
    @DisplayName("consultar deve aprovar quando o CPF nao tem sufixo de excecao")
    void consultar_deveAprovar_quandoSemSufixoDeExcecao() {
        ResultadoBureau resultado = service.consultar(TipoBureau.SERASA, "98765432198");

        assertThat(resultado).isEqualTo(ResultadoBureau.APROVADO);
        assertThat(resultado.isAprovado()).isTrue();
    }
}
