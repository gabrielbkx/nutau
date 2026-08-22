package br.com.nutau.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ErrorCode")
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("toda constante deve ter status e mensagem preenchidos")
    void constante_deveTerStatusEMensagem_sempre(ErrorCode codigo) {
        assertThat(codigo.getHttpStatus()).isNotNull();
        assertThat(codigo.getMensagem()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("apenas falhas transitorias devem ser marcadas como retentaveis")
    void constante_deveSerRetentavel_apenasQuandoTransitoria(ErrorCode codigo) {
        // Retentar um 4xx e desperdicio garantido: a requisicao esta errada e vai
        // continuar errada. So faz sentido insistir em falha do lado do servidor.
        if (codigo.isRetentavel()) {
            assertThat(codigo.getHttpStatus().is5xxServerError())
                    .as("%s e retentavel mas nao e 5xx", codigo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("NutauException deve herdar status e mensagem do codigo")
    void nutauException_deveHerdarDoCodigo() {
        NutauException e = new NutauException(ErrorCode.SOLICITACAO_NAO_ENCONTRADA);

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SOLICITACAO_NAO_ENCONTRADA);
        assertThat(e.getMessage()).isEqualTo(ErrorCode.SOLICITACAO_NAO_ENCONTRADA.getMensagem());
        assertThat(e.isRetentavel()).isFalse();
    }

    @Test
    @DisplayName("o detalhe interno nao deve vazar para a mensagem publica")
    void nutauException_naoDeveVazarDetalheInterno() {
        NutauException e = new NutauException(
                ErrorCode.BUREAU_INDISPONIVEL, "timeout no host bureau-interno:8443");

        // getMessage vai para o log; getMensagemPublica vai para o cliente.
        assertThat(e.getMessage()).contains("bureau-interno:8443");
        assertThat(e.getMensagemPublica()).doesNotContain("bureau-interno");
    }
}
