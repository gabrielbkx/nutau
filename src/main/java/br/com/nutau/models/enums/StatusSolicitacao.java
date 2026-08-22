package br.com.nutau.models.enums;

/**
 * Estagio do atendimento de uma solicitacao de credito.
 *
 * <p>O ciclo de vida e sempre {@code EM_ANALISE} -> estado final. Uma solicitacao
 * finalizada nunca volta para analise: um novo pedido gera uma nova linha.
 */
public enum StatusSolicitacao {

    /** Pedido registrado, consultas aos bureaus em andamento. Unico estado nao final. */
    EM_ANALISE,

    /** Todos os bureaus aprovaram e o valor concedido foi o integral. */
    APROVADO,

    /**
     * Aprovado, porem por valor menor que o pedido.
     *
     * <p>Estado proprio, e nao um {@code APROVADO} com observacao no motivo: o cliente
     * conseguiu credito, mas nao o que queria, e um front-end precisa distinguir os dois
     * casos sem interpretar texto livre. So ocorre em pedidos de aumento de limite.
     */
    APROVADO_PARCIAL,

    /** Ao menos um bureau reprovou. */
    REPROVADO,

    /**
     * A analise nao pode ser concluida por falha tecnica - por exemplo, um bureau
     * indisponivel apos todas as retentativas. Nao e uma negativa de credito:
     * e a ausencia de resposta, e exige intervencao.
     */
    ERRO_ANALISE;

    /** Todo estado diferente de {@code EM_ANALISE} encerra a solicitacao. */
    public boolean isFinal() {
        return this != EM_ANALISE;
    }

    /** Verdadeiro para aprovacao integral ou parcial - ambas concedem credito. */
    public boolean concedeuCredito() {
        return this == APROVADO || this == APROVADO_PARCIAL;
    }
}
