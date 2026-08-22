package br.com.nutau.repositories;

import br.com.nutau.models.entities.ConsultaBureau;
import br.com.nutau.models.enums.TipoBureau;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaBureauRepository extends JpaRepository<ConsultaBureau, UUID> {

    Optional<ConsultaBureau> findBySolicitacaoIdAndBureau(UUID solicitacaoId, TipoBureau bureau);

    List<ConsultaBureau> findBySolicitacaoId(UUID solicitacaoId);
}
