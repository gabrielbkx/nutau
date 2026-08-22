package br.com.nutau.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Pedido de aumento de limite.
 *
 * <p>Nao existe DTO equivalente para a emissao do primeiro cartao, e isso e proposital:
 * la o cliente nao informa nada. Com dois contratos separados, o {@code @NotNull} abaixo
 * pode ser incondicional - a alternativa seria um DTO unico com valor obrigatorio "so as
 * vezes", regra que o Bean Validation so expressa de forma tortuosa.
 */
@Schema(description = "Pedido de aumento de limite sobre o cartao existente")
public record AumentoLimiteRequest(
        /*
         * BigDecimal, nunca double: dinheiro em ponto flutuante binario acumula erro de
         * arredondamento (0.1 + 0.2 != 0.3). @Digits fixa a escala em 2 casas para que
         * o valor recebido case exatamente com numeric(15,2) no banco.
         */
        @Schema(example = "8000.00")
        @NotNull(message = "Valor solicitado e obrigatorio")
        @DecimalMin(value = "100.00", message = "O valor minimo e R$ 100,00")
        @DecimalMax(value = "1000000.00", message = "O valor maximo e R$ 1.000.000,00")
        @Digits(integer = 13, fraction = 2, message = "Valor deve ter no maximo 2 casas decimais")
        BigDecimal valorSolicitado) {
}
