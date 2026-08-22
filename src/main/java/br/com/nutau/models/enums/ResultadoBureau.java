package br.com.nutau.models.enums;

/**
 * Veredito devolvido por um bureau de credito.
 *
 * <p>Modelar o resultado como enum, e nao como {@code boolean}, permite saber
 * <em>por que</em> o credito foi negado - informacao necessaria para o e-mail
 * enviado ao cliente no fim do fluxo.
 */
public enum ResultadoBureau {

    /** Sem restricoes. */
    APROVADO(true, "Sem restricoes encontradas"),

    /** Score abaixo do minimo aceito (regra do Serasa neste simulador). */
    SCORE_BAIXO(false, "Score de credito abaixo do minimo exigido"),

    /** Registro de inadimplencia ativo (regra do SPC neste simulador). */
    NOME_SUJO(false, "Restricao financeira ativa em seu CPF");

    private final boolean aprovado;
    private final String descricao;

    ResultadoBureau(boolean aprovado, String descricao) {
        this.aprovado = aprovado;
        this.descricao = descricao;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public String getDescricao() {
        return descricao;
    }
}
