package br.com.nutau.models.entities;

import br.com.nutau.models.enums.StatusSolicitacao;
import br.com.nutau.models.enums.TipoSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pedido de limite de credito aberto por um cliente. */
@Entity
@Table(name = "solicitacoes_credito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoCredito {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * LAZY de proposito: a maior parte das operacoes sobre a solicitacao nao precisa
     * carregar o usuario inteiro. Com open-in-view desligado no application.yml,
     * qualquer acesso fora da transacao falha alto em vez de disparar N+1 silencioso.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoSolicitacao tipo;

    /**
     * Valor pedido pelo cliente.
     *
     * <p>Nulo em {@code EMISSAO_CARTAO}: no primeiro pedido o cliente nao escolhe quanto
     * quer - quem arbitra o limite inicial e o banco, a partir da renda declarada.
     */
    @Column(name = "valor_solicitado", precision = 15, scale = 2)
    private BigDecimal valorSolicitado;

    /**
     * Valor efetivamente concedido. Nulo enquanto a analise nao termina.
     *
     * <p>Pode ser menor que o solicitado quando o teto de renda limita o pedido - nesse
     * caso o status final e {@code APROVADO_PARCIAL}.
     */
    @Column(name = "valor_aprovado", precision = 15, scale = 2)
    private BigDecimal valorAprovado;

    /**
     * Cartao a que o pedido se refere.
     *
     * <p>Em {@code AUMENTO_LIMITE} aponta para o cartao existente desde o inicio. Em
     * {@code EMISSAO_CARTAO} permanece nulo ate a aprovacao, quando o cartao e criado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id")
    private CartaoCredito cartao;

    /**
     * STRING, nunca ORDINAL. Com ORDINAL o banco guarda a posicao do enum, e inserir
     * um valor novo no meio da lista corromperia todos os registros existentes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusSolicitacao status;

    /** Justificativa exibida ao cliente. Nulo enquanto a analise nao termina. */
    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "finalizado_em")
    private OffsetDateTime finalizadoEm;

    /** Encerra a solicitacao sem conceder credito (reprovacao ou falha tecnica). */
    public void finalizar(StatusSolicitacao statusFinal, String motivo) {
        finalizar(statusFinal, motivo, null);
    }

    /** Aplica o veredito final, registrando o valor concedido e o instante de conclusao. */
    public void finalizar(
            StatusSolicitacao statusFinal, String motivo, BigDecimal valorAprovado) {
        if (!statusFinal.isFinal()) {
            throw new IllegalArgumentException("Status de finalizacao invalido: " + statusFinal);
        }
        if (statusFinal.concedeuCredito() && valorAprovado == null) {
            throw new IllegalArgumentException(
                    "Status " + statusFinal + " exige um valor aprovado");
        }
        this.status = statusFinal;
        this.motivo = motivo;
        this.valorAprovado = valorAprovado;
        this.finalizadoEm = OffsetDateTime.now();
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = StatusSolicitacao.EM_ANALISE;
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
