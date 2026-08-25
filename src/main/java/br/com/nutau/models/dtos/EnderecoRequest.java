package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Endereco informado na abertura da conta.
 *
 * <p>Sao dois campos obrigatorios, e nao sete: logradouro, bairro, cidade e UF sao
 * derivados do CEP pelo ViaCEP. Pedir ao cliente o que ja da para descobrir e digitacao
 * inutil - e cada campo digitado a mais e uma chance a mais de o endereco entrar no banco
 * com "Av." num cadastro e "Avenida" no outro.
 */
@Schema(description = "Endereco do cliente: informe apenas CEP e numero")
public record EnderecoRequest(
        /*
         * Formato apenas, como no CPF. Se o CEP existe de fato e o ViaCEP quem responde -
         * conferir isso aqui exigiria manter uma copia da base dos Correios.
         */
        @Schema(example = "01001-000")
        @NotBlank(message = "CEP e obrigatorio")
        @Pattern(
                regexp = "\\d{5}-?\\d{3}",
                message = "CEP invalido: informe 8 digitos, com ou sem hifen")
        String cep,

        /*
         * String, nao numero inteiro: "1500-A", "S/N" e "12 B" sao numeros de endereco
         * validos, e nenhum deles cabe num int.
         */
        @Schema(example = "1500")
        @NotBlank(message = "Numero e obrigatorio")
        @Size(max = 10, message = "Numero deve ter no maximo 10 caracteres")
        String numero,

        @Schema(example = "Apto 42", description = "Opcional")
        @Size(max = 60, message = "Complemento deve ter no maximo 60 caracteres")
        String complemento) {
}
