package br.com.nutau.security;

import br.com.nutau.models.entities.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adaptador entre a entidade {@link Usuario} e o contrato do Spring Security.
 *
 * <p>A entidade poderia implementar {@code UserDetails} diretamente - e muito codigo por
 * ai faz isso. Optei por separar: assim o modelo de dominio nao carrega metodos como
 * {@code isAccountNonLocked()} que nada tem a ver com credito, e trocar o mecanismo de
 * autenticacao no futuro nao mexe na tabela do banco.
 */
public class UsuarioAutenticado implements UserDetails {

    private static final List<GrantedAuthority> AUTORIDADES =
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));

    private final UUID id;
    private final String nome;
    private final String cpf;
    private final String senhaHash;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.cpf = usuario.getCpf();
        this.senhaHash = usuario.getSenha();
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTORIDADES;
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    /** O CPF e o "username" deste sistema: e por ele que o cliente faz login. */
    @Override
    public String getUsername() {
        return cpf;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
