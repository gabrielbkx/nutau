package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Endereco completo do cliente, ja com os campos preenchidos pelo ViaCEP.
 *
 * <p>O cliente enviou dois campos e recebe sete de volta: e assim que ele confere, ainda
 * na resposta do cadastro, se o CEP que digitou aponta mesmo para a rua onde mora.
 */
@Schema(description = "Endereco completo do cliente")
public record EnderecoResponse(
        @Schema(description = "CEP formatado", example = "01001-000")
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf) {
}
