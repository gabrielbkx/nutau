package br.com.nutau.mappers;

import br.com.nutau.models.dtos.CartaoResponse;
import br.com.nutau.models.entities.CartaoCredito;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Conversao de {@link CartaoCredito} para o DTO de resposta. */
@Mapper
public interface CartaoMapper {

    /**
     * Monta a resposta com os beneficios da categoria.
     *
     * <p>Anuidade, cashback e nome comercial nao sao colunas do cartao: vem do enum
     * {@code CategoriaCartao}, e o MapStruct navega ate eles pelo caminho da propriedade.
     * Nada disso e duplicado no banco - a categoria e a unica fonte de verdade.
     *
     * @param limiteMaximo teto calculado sobre a renda atual do cliente, informado pelo
     *     service: e valor derivado, e derivado nao se armazena
     */
    @Mapping(target = "id", source = "cartao.id")
    @Mapping(target = "numeroMascarado",
            source = "cartao.ultimosDigitos", qualifiedByName = "mascararNumero")
    @Mapping(target = "categoria", source = "cartao.categoria")
    @Mapping(target = "nomeCategoria", source = "cartao.categoria.nomeExibicao")
    @Mapping(target = "limite", source = "cartao.limite")
    @Mapping(target = "limiteMaximo", source = "limiteMaximo")
    @Mapping(target = "anuidade", source = "cartao.categoria.anuidade")
    @Mapping(target = "cashbackPercentual", source = "cartao.categoria.cashbackPercentual")
    @Mapping(target = "status", source = "cartao.status")
    @Mapping(target = "validadeAte", source = "cartao.validadeAte")
    @Mapping(target = "criadoEm", source = "cartao.criadoEm")
    CartaoResponse toResponse(CartaoCredito cartao, BigDecimal limiteMaximo);

    @Named("mascararNumero")
    default String mascararNumero(String ultimosDigitos) {
        return ultimosDigitos == null ? null : "**** **** **** " + ultimosDigitos;
    }
}
