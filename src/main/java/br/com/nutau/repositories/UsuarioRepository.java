package br.com.nutau.repositories;

import br.com.nutau.models.entities.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** O CPF e o identificador de login. */
    Optional<Usuario> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
