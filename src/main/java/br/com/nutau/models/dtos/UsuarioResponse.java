package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representacao publica de um cliente.
 *
 * <p>Repare no que <b>nao</b> esta aqui: a senha. Este e o motivo de existir um DTO em vez
 * de serializar a entidade direto - um campo novo na entidade nunca vaza pela API sozinho.
 */
@Schema(description = "Dados publicos do cliente")
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        @Schema(description = "CPF mascarado", example = "***.456.789-**")
        String cpf,
        BigDecimal rendaMensal,
        EnderecoResponse endereco,
        OffsetDateTime criadoEm) {
}
