package br.com.nutau.repositories;

import br.com.nutau.models.entities.SolicitacaoCredito;
import br.com.nutau.models.enums.StatusSolicitacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitacaoCreditoRepository
        extends JpaRepository<SolicitacaoCredito, UUID> {

    /**
     * Busca escopada pelo dono.
     *
     * <p>Filtrar por usuario dentro da query - e nao carregar por ID e comparar depois -
     * fecha a porta para IDOR: um cliente autenticado nao consegue ler a solicitacao de
     * outro apenas trocando o UUID na URL.
     */
    Optional<SolicitacaoCredito> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<SolicitacaoCredito> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    boolean existsByUsuarioIdAndStatus(UUID usuarioId, StatusSolicitacao status);
}
