package br.com.nutau.models.entities;

import br.com.nutau.models.enums.ResultadoBureau;
import br.com.nutau.models.enums.TipoBureau;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resultado parcial de um bureau para uma solicitacao.
 *
 * <p>Existe uma restricao unica em (solicitacao_id, bureau): se a mesma tarefa for
 * reentregue pelo RabbitMQ - o que e esperado numa entrega "at least once" - o banco
 * recusa a duplicata em vez de gravar dois resultados para a mesma consulta.
 */
@Entity
@Table(name = "consultas_bureau")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaBureau {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoCredito solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "bureau", nullable = false, length = 20)
    private TipoBureau bureau;

    /** Nulo ate o bureau responder. */
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 30)
    private ResultadoBureau resultado;

    @Column(name = "aprovado")
    private Boolean aprovado;

    /** Quantas vezes a consulta foi tentada, incluindo as que falharam. */
    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "finalizado_em")
    private OffsetDateTime finalizadoEm;

    /** Registra a resposta do bureau e fecha a consulta. */
    public void registrarResultado(ResultadoBureau resultado) {
        this.resultado = resultado;
        this.aprovado = resultado.isAprovado();
        this.finalizadoEm = OffsetDateTime.now();
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        criadoEm = OffsetDateTime.now();
    }
}
