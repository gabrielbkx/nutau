package br.com.nutau.mappers;

import br.com.nutau.models.dtos.SolicitacaoCreditoResponse;
import br.com.nutau.models.entities.SolicitacaoCredito;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Conversao de {@link SolicitacaoCredito} para o DTO de resposta.
 *
 * <p>Nenhum {@code @Mapping} aqui: os nomes coincidem e o MapStruct resolve sozinho. E o
 * caso em que o mapper de fato elimina codigo - o metodo estatico anterior repetia oito
 * getters em ordem, e trocar dois de lugar seria um bug que nenhum compilador pegaria.
 */
@Mapper
public interface SolicitacaoMapper {

    SolicitacaoCreditoResponse toResponse(SolicitacaoCredito solicitacao);

    /** O mapper de lista e gerado a partir do de item, sem stream na mao. */
    List<SolicitacaoCreditoResponse> toResponseList(List<SolicitacaoCredito> solicitacoes);
}
