package br.com.nutau.services;

import br.com.nutau.models.dtos.LoginRequest;
import br.com.nutau.models.dtos.TokenResponse;
import br.com.nutau.security.JwtService;
import br.com.nutau.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Login e emissao de token (US02). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Valida as credenciais e devolve um token.
     *
     * <p>A comparacao de senha e delegada ao {@code AuthenticationManager} de proposito.
     * Alem de evitar codigo duplicado, o Spring executa o BCrypt mesmo quando o CPF nao
     * existe, mantendo o tempo de resposta constante. Um login que responde na hora para
     * CPF inexistente e devagar para CPF existente entrega a lista de clientes do banco
     * via timing attack.
     */
    public TokenResponse autenticar(LoginRequest request) {
        String cpf = CpfHelper.normalizar(request.cpf());

        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cpf, request.senha()));

        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        log.info("Login efetuado: usuarioId={}", usuario.getId());

        return TokenResponse.bearer(
                jwtService.gerarToken(usuario), jwtService.calcularExpiracao());
    }
}
