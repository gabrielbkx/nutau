package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Token emitido apos autenticacao bem-sucedida. */
@Schema(description = "Token de acesso JWT")
public record TokenResponse(
        @Schema(description = "JWT assinado, enviado no header Authorization")
        String token,

        @Schema(description = "Esquema de autenticacao", example = "Bearer")
        String tipo,

        @Schema(description = "Instante em que o token deixa de valer")
        OffsetDateTime expiraEm) {

    public static TokenResponse bearer(String token, OffsetDateTime expiraEm) {
        return new TokenResponse(token, "Bearer", expiraEm);
    }
}
