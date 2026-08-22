package br.com.nutau.models.dtos;

import br.com.nutau.models.enums.CategoriaCartao;
import br.com.nutau.models.enums.StatusCartao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Cartao de credito do cliente. */
@Schema(description = "Cartao de credito, categoria e limite atual")
public record CartaoResponse(
        UUID id,
        @Schema(description = "Exibicao mascarada do cartao", example = "**** **** **** 4821")
        String numeroMascarado,
        @Schema(example = "VIOLETA")
        CategoriaCartao categoria,
        @Schema(description = "Nome comercial da categoria", example = "Nutaú Violeta")
        String nomeCategoria,
        BigDecimal limite,
        @Schema(description = "Teto que a categoria admite para a renda declarada")
        BigDecimal limiteMaximo,
        BigDecimal anuidade,
        @Schema(description = "Cashback em pontos percentuais", example = "1")
        BigDecimal cashbackPercentual,
        StatusCartao status,
        LocalDate validadeAte,
        OffsetDateTime criadoEm) {

}
