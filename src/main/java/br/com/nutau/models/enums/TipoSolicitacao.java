package br.com.nutau.models.enums;

/**
 * Natureza do pedido de credito.
 *
 * <p>A analise nos bureaus e identica nos dois casos - o que muda e a pre-condicao para
 * abrir o pedido e o efeito da aprovacao.
 */
public enum TipoSolicitacao {

    /**
     * Primeiro cartao do cliente. Ele nao informa valor: o limite inicial e arbitrado
     * pelo banco a partir da renda declarada. Exige que o cliente ainda nao tenha cartao.
     */
    EMISSAO_CARTAO,

    /**
     * Aumento de limite sobre um cartao existente. Aqui o valor solicitado faz sentido -
     * o cliente ja tem uma referencia e pede mais que ela.
     */
    AUMENTO_LIMITE;

    /** Indica se o cliente informa o valor desejado neste tipo de pedido. */
    public boolean exigeValorSolicitado() {
        return this == AUMENTO_LIMITE;
    }
}
