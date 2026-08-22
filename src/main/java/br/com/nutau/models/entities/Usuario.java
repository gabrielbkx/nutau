package br.com.nutau.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

/** Cliente cadastrado no banco. */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    /**
     * O identificador e gerado pela aplicacao, nao pelo banco. Isso permite conhecer
     * o ID antes do INSERT - util quando o ID precisa entrar numa mensagem publicada
     * na mesma transacao. UUID tambem evita expor o volume de clientes do banco,
     * o que um ID sequencial entregaria de graca.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Somente digitos, sem pontos nem hifen. A normalizacao acontece no service. */
    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    /** Hash BCrypt. Nunca a senha em texto puro. */
    @Column(name = "senha", nullable = false, length = 72)
    private String senha;

    /**
     * Renda mensal declarada na abertura da conta.
     *
     * <p>E a base do calculo do limite de credito. Declarada pelo cliente e aceita sem
     * comprovacao - um banco real cruzaria com a Receita, o que esta fora do escopo
     * deste simulador.
     */
    @Column(name = "renda_mensal", nullable = false, precision = 15, scale = 2)
    private BigDecimal rendaMensal;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
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
