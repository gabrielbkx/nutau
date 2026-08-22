package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Dados de abertura de conta (US01). */
@Schema(description = "Dados para cadastro de um novo cliente")
public record CadastroRequest(
        @Schema(example = "Maria Souza")
        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
        String nome,

        @Schema(example = "maria@exemplo.com")
        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 255)
        String email,

        @Schema(example = "123.456.789-01")
        @NotBlank(message = "CPF e obrigatorio")
        /*
         * Validacao apenas de formato, como a especificacao pede. Os digitos
         * verificadores nao sao conferidos de proposito: a regra de negocio do bureau
         * depende dos dois ultimos digitos do CPF (00, 11 e 22 disparam cenarios
         * distintos), e exigir DV valido tornaria esses cenarios dificeis de reproduzir
         * sem nenhum ganho. O lookahead recusa as 11 posicoes com o mesmo digito.
         */
        @Pattern(regexp = "(?!(\\d)\\1{2}\\.?\\1{3}\\.?\\1{3}-?\\1{2}$)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF invalido: informe 11 digitos, com ou sem mascara")
        String cpf,

        /*
         * O limite superior existe por causa do BCrypt: ele so considera os primeiros
         * 72 bytes da senha e silenciosamente ignora o resto. Recusar antes e mais
         * honesto do que aceitar uma senha longa e autenticar por um prefixo dela.
         */
        @Schema(example = "senhaSegura123")
        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String senha,

        /*
         * Renda declarada, sem comprovacao - e o que sustenta o calculo do limite.
         * Um banco real cruzaria com a Receita antes de conceder qualquer coisa.
         */
        @Schema(example = "4000.00")
        @NotNull(message = "Renda mensal e obrigatoria")
        @DecimalMin(value = "500.00", message = "A renda minima aceita e R$ 500,00")
        @Digits(integer = 13, fraction = 2, message = "Renda deve ter no maximo 2 casas decimais")
        BigDecimal rendaMensal) {
}
