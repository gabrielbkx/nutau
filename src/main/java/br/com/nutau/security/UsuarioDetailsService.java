package br.com.nutau.security;

import br.com.nutau.services.CpfHelper;
import br.com.nutau.repositories.UsuarioRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o usuario para o Spring Security, no login e a cada requisicao autenticada. */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Usado pelo {@code AuthenticationManager} durante o login. O "username" aqui e o CPF.
     *
     * <p>A mensagem de erro e propositalmente generica: informar "CPF nao cadastrado"
     * transformaria o endpoint de login em um verificador de quem e cliente do banco.
     */
    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado loadUserByUsername(String cpf) {
        return usuarioRepository.findByCpf(CpfHelper.normalizar(cpf))
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));
    }

    /** Usado pelo filtro JWT, onde o token carrega o ID e nao o CPF. */
    @Transactional(readOnly = true)
    public UsuarioAutenticado carregarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }
}
