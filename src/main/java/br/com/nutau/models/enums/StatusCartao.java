package br.com.nutau.models.enums;

/** Situacao operacional do cartao. */
public enum StatusCartao {

    ATIVO,
    BLOQUEADO,
    CANCELADO;

    /** Somente um cartao ativo aceita pedido de aumento de limite. */
    public boolean permiteAumentoLimite() {
        return this == ATIVO;
    }
}
