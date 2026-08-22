package br.com.nutau.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Le o header {@code Authorization: Bearer <token>} e popula o contexto de seguranca.
 *
 * <p>Estende {@code OncePerRequestFilter} e nao {@code GenericFilterBean}: sem essa
 * garantia, um {@code forward} interno do servlet faria o filtro rodar de novo na mesma
 * requisicao - um bug classico e chato de diagnosticar.
 *
 * <p>O filtro nunca bloqueia requisicao. Ele apenas <em>identifica</em> quem esta batendo
 * na porta. Decidir quem pode entrar e trabalho da {@link SecurityConfig}. Misturar as
 * duas responsabilidades e o erro mais comum nesse ponto: um filtro que devolve 401
 * sozinho quebra todas as rotas publicas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        extrairToken(request)
                .flatMap(jwtService::extrairUsuarioId)
                .ifPresent(usuarioId -> autenticar(usuarioId.toString(), request));

        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIXO_BEARER)) {
            return java.util.Optional.empty();
        }
        String token = header.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }

    private void autenticar(String usuarioId, HttpServletRequest request) {
        // Se algo ja autenticou esta requisicao, nao sobrescrevemos.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        try {
            UsuarioAutenticado usuario =
                    usuarioDetailsService.carregarPorId(java.util.UUID.fromString(usuarioId));

            var authentication = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException e) {
            // Token assinado por nos, mas o usuario nao existe mais (conta removida).
            // A requisicao segue sem autenticacao e termina em 401.
            log.debug("Token valido para usuario inexistente: {}", usuarioId);
        }
    }
}
