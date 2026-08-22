package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Credenciais de acesso (US02). */
@Schema(description = "Credenciais de login")
public record LoginRequest(
        @Schema(example = "123.456.789-01")
        @NotBlank(message = "CPF e obrigatorio")
        @Pattern(regexp = "(?!(\\d)\\1{2}\\.?\\1{3}\\.?\\1{3}-?\\1{2}$)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF invalido: informe 11 digitos, com ou sem mascara")
        String cpf,

        @Schema(example = "senhaSegura123")
        @NotBlank(message = "Senha e obrigatoria")
        String senha) {
}
