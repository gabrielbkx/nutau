package br.com.nutau.models.dtos;

/**
 * Envelope padrao de resposta de sucesso da API.
 *
 * <p>Todo endpoint devolve o conteudo sob a chave {@code data}. Envelopar tem um custo -
 * um nivel a mais de aninhamento no JSON - e uma vantagem que compensa: o formato da
 * resposta nao muda quando o retorno passa de objeto para lista, e ha lugar previsto para
 * acrescentar metadados (paginacao, por exemplo) sem quebrar quem ja consome a API.
 *
 * @param data conteudo da resposta
 * @param <T> tipo do conteudo
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
