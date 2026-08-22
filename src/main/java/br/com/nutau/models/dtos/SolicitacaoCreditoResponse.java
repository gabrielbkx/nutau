package br.com.nutau.models.dtos;

import br.com.nutau.models.enums.StatusSolicitacao;
import br.com.nutau.models.enums.TipoSolicitacao;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Estado atual de uma solicitacao de credito. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Situacao de uma solicitacao de credito")
public record SolicitacaoCreditoResponse(
        UUID id,
        TipoSolicitacao tipo,
        @Schema(description = "Valor pedido. Ausente na emissao do primeiro cartao.")
        BigDecimal valorSolicitado,
        @Schema(description = "Valor concedido. Ausente enquanto a analise nao termina.")
        BigDecimal valorAprovado,
        StatusSolicitacao status,
        @Schema(description = "Justificativa do veredito. Ausente enquanto EM_ANALISE.")
        String motivo,
        OffsetDateTime criadoEm,
        OffsetDateTime finalizadoEm) {
}
