package br.com.nutau.security;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.models.dtos.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Responde 401 em JSON quando a requisicao chega sem autenticacao valida.
 *
 * <p>Sem isso, o Spring Security devolveria uma pagina HTML de erro do container - um
 * cliente que espera JSON quebraria ao tentar desserializar a resposta de falha.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErroResponse corpo =
                ErroResponse.de(ErrorCode.NAO_AUTENTICADO, request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
