package br.com.nutau.models.entities;

import br.com.nutau.models.enums.CategoriaCartao;
import br.com.nutau.models.enums.StatusCartao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cartao de credito do cliente.
 *
 * <p>E o objeto sobre o qual a analise recai: antes desta entidade existir, uma
 * solicitacao aprovada nao produzia nada de concreto no sistema.
 */
@Entity
@Table(name = "cartoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaoCredito {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Um cartao por cliente. A unicidade e reforcada por constraint no banco. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /**
     * Apenas os quatro ultimos digitos.
     *
     * <p>O numero completo nunca e gerado nem armazenado. Guardar PAN de cartao sujeita o
     * sistema inteiro ao escopo do PCI-DSS, e nada aqui precisa dele: para exibir
     * "Cartao final 4821" quatro digitos bastam.
     */
    @Column(name = "ultimos_digitos", nullable = false, length = 4)
    private String ultimosDigitos;

    /**
     * Faixa do cartao. Determina percentuais de limite, anuidade e cashback.
     *
     * <p>Coluna comum, e nao discriminator de heranca: promover o cartao de categoria e
     * um UPDATE nesta coluna, sem trocar a linha nem o id.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaCartao categoria;

    @Column(name = "limite", nullable = false, precision = 15, scale = 2)
    private BigDecimal limite;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusCartao status;

    @Column(name = "validade_ate", nullable = false)
    private LocalDate validadeAte;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Eleva o limite. Rejeita reducao para que um bug nunca diminua credito concedido. */
    public void elevarLimitePara(BigDecimal novoLimite) {
        if (novoLimite == null || novoLimite.compareTo(limite) <= 0) {
            throw new IllegalArgumentException(
                    "O novo limite deve ser maior que o atual: " + novoLimite + " <= " + limite);
        }
        this.limite = novoLimite;
    }

    /**
     * Promove o cartao para uma categoria superior.
     *
     * <p>Rebaixamento e recusado: tirar beneficio ja concedido a um cliente e o tipo de
     * efeito que nunca deve acontecer por acidente. Se um dia for necessario, sera uma
     * operacao explicita e com nome proprio.
     */
    public void promoverPara(CategoriaCartao novaCategoria) {
        if (novaCategoria == null || !novaCategoria.superiorA(categoria)) {
            throw new IllegalArgumentException(
                    "Nova categoria deve ser superior a atual: " + novaCategoria
                            + " nao supera " + categoria);
        }
        this.categoria = novaCategoria;
    }

    public boolean isAtivo() {
        return status == StatusCartao.ATIVO;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = StatusCartao.ATIVO;
        }
        if (categoria == null) {
            categoria = CategoriaCartao.INICIAL;
        }
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = OffsetDateTime.now();
    }
}
