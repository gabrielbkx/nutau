package br.com.nutau.models.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Categorias de cartao do Nutau, da entrada ao topo.
 *
 * <p>Cada categoria carrega os parametros da sua faixa: percentual de renda concedido na
 * emissao, percentual maximo alcancavel por aumentos, teto absoluto, anuidade e cashback.
 *
 * <p><b>Por que um enum e nao uma hierarquia de classes.</b> As categorias diferem em
 * parametros, nao em estrutura - e modelar isso com heranca JPA criaria um problema serio:
 * uma entidade JPA nao pode trocar de tipo. Promover um cartao de AMBAR para VIOLETA
 * exigiria apagar e recriar a linha, gerando novo id e quebrando as solicitacoes que
 * apontam para ela. Com a categoria como coluna, promover e um UPDATE.
 *
 * <p>Se algum dia o <em>comportamento</em> divergir de verdade entre categorias - e nao
 * apenas os numeros - o caminho e uma interface de politica com uma implementacao por
 * categoria, resolvida por um {@code Map<CategoriaCartao, PoliticaCartao>}. Enquanto a
 * diferenca for so de parametro, criar quatro classes que devolvem constantes seria
 * cerimonia sem beneficio.
 *
 * <p>Os dois percentuais existem por necessidade: com um so, o limite inicial ja seria o
 * maximo da categoria e nenhum pedido de aumento teria para onde ir.
 */
public enum CategoriaCartao {

    /** Entrada. Sem anuidade e sem cashback - o cartao de quem esta comecando. */
    INICIAL(
            "Nutaú Início",
            new BigDecimal("0.00"),
            new BigDecimal("0.30"),
            new BigDecimal("0.50"),
            new BigDecimal("3000.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.000")),

    AMBAR(
            "Nutaú Âmbar",
            new BigDecimal("3000.00"),
            new BigDecimal("0.35"),
            new BigDecimal("0.70"),
            new BigDecimal("10000.00"),
            new BigDecimal("19.90"),
            new BigDecimal("0.005")),

    VIOLETA(
            "Nutaú Violeta",
            new BigDecimal("7000.00"),
            new BigDecimal("0.40"),
            new BigDecimal("1.00"),
            new BigDecimal("30000.00"),
            new BigDecimal("39.90"),
            new BigDecimal("0.010")),

    /** Topo. Anuidade isenta como beneficio da faixa, nao por ser categoria de entrada. */
    INFINITO(
            "Nutaú Infinito",
            new BigDecimal("15000.00"),
            new BigDecimal("0.50"),
            new BigDecimal("1.50"),
            new BigDecimal("100000.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.015"));

    private final String nomeExibicao;
    private final BigDecimal rendaMinima;
    private final BigDecimal percentualEmissao;
    private final BigDecimal percentualMaximo;
    private final BigDecimal tetoAbsoluto;
    private final BigDecimal anuidade;
    private final BigDecimal cashback;

    CategoriaCartao(
            String nomeExibicao,
            BigDecimal rendaMinima,
            BigDecimal percentualEmissao,
            BigDecimal percentualMaximo,
            BigDecimal tetoAbsoluto,
            BigDecimal anuidade,
            BigDecimal cashback) {
        this.nomeExibicao = nomeExibicao;
        this.rendaMinima = rendaMinima;
        this.percentualEmissao = percentualEmissao;
        this.percentualMaximo = percentualMaximo;
        this.tetoAbsoluto = tetoAbsoluto;
        this.anuidade = anuidade;
        this.cashback = cashback;
    }

    /**
     * Categoria correspondente a uma renda.
     *
     * <p>Percorre da mais alta para a mais baixa e devolve a primeira que a renda alcanca.
     * {@code INICIAL} tem renda minima zero, entao sempre ha uma resposta - a busca nunca
     * termina vazia.
     *
     * <p>Na Fase 2 esta decisao deixara de depender so da renda: o score devolvido pelos
     * bureaus tambem entrara na conta, e este e o unico ponto que precisara mudar.
     */
    public static CategoriaCartao paraRenda(BigDecimal rendaMensal) {
        if (rendaMensal == null) {
            throw new IllegalArgumentException("Renda mensal e obrigatoria para definir a categoria");
        }
        return Arrays.stream(values())
                .sorted(Comparator.comparing(CategoriaCartao::getRendaMinima).reversed())
                .filter(categoria -> rendaMensal.compareTo(categoria.rendaMinima) >= 0)
                .findFirst()
                .orElse(INICIAL);
    }

    /**
     * Limite concedido na emissao do cartao, ja respeitando o teto da categoria.
     *
     * <p>{@code RoundingMode.DOWN} sempre: na duvida sobre o centavo, o banco concede a
     * menos, nunca a mais.
     */
    public BigDecimal calcularLimiteEmissao(BigDecimal rendaMensal) {
        return rendaMensal
                .multiply(percentualEmissao)
                .setScale(2, RoundingMode.DOWN)
                .min(tetoAbsoluto);
    }

    /** Limite maximo que esta categoria admite para a renda informada. */
    public BigDecimal calcularLimiteMaximo(BigDecimal rendaMensal) {
        return rendaMensal
                .multiply(percentualMaximo)
                .setScale(2, RoundingMode.DOWN)
                .min(tetoAbsoluto);
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public BigDecimal getRendaMinima() {
        return rendaMinima;
    }

    public BigDecimal getPercentualEmissao() {
        return percentualEmissao;
    }

    public BigDecimal getPercentualMaximo() {
        return percentualMaximo;
    }

    public BigDecimal getTetoAbsoluto() {
        return tetoAbsoluto;
    }

    public BigDecimal getAnuidade() {
        return anuidade;
    }

    public BigDecimal getCashback() {
        return cashback;
    }

    /** Cashback em pontos percentuais, para exibicao ao cliente. */
    public BigDecimal getCashbackPercentual() {
        return cashback.multiply(new BigDecimal("100")).stripTrailingZeros();
    }

    public boolean isIsentaDeAnuidade() {
        return anuidade.compareTo(BigDecimal.ZERO) == 0;
    }

    /** Verdadeiro se esta categoria e superior a informada. */
    public boolean superiorA(CategoriaCartao outra) {
        return this.rendaMinima.compareTo(outra.rendaMinima) > 0;
    }
}
