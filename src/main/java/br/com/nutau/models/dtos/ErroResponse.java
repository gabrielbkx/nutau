package br.com.nutau.models.dtos;

import br.com.nutau.exceptions.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato unico de erro da API.
 *
 * <p>O campo {@code codigo} carrega o nome da constante de {@link ErrorCode}. E ele que o
 * cliente deve inspecionar para decidir o que fazer - a {@code mensagem} existe para ser
 * exibida ao usuario e pode ser reescrita a qualquer momento sem quebrar ninguem.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padronizada de erro")
public record ErroResponse(
        OffsetDateTime timestamp,
        int status,
        @Schema(description = "Codigo estavel do erro", example = "CREDENCIAIS_INVALIDAS")
        String codigo,
        String mensagem,
        String caminho,
        @Schema(description = "Detalhamento por campo. Presente apenas em erro de validacao.")
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ErroResponse de(ErrorCode errorCode, String caminho) {
        return montar(errorCode, errorCode.getMensagem(), caminho, null);
    }

    public static ErroResponse de(ErrorCode errorCode, String mensagem, String caminho) {
        return montar(errorCode, mensagem, caminho, null);
    }

    public static ErroResponse deValidacao(String caminho, List<CampoInvalido> campos) {
        return montar(
                ErrorCode.CAMPOS_INVALIDOS,
                ErrorCode.CAMPOS_INVALIDOS.getMensagem(),
                caminho,
                campos);
    }

    private static ErroResponse montar(
            ErrorCode errorCode, String mensagem, String caminho, List<CampoInvalido> campos) {
        return new ErroResponse(
                OffsetDateTime.now(),
                errorCode.getHttpStatus().value(),
                errorCode.name(),
                mensagem,
                caminho,
                campos);
    }
}
