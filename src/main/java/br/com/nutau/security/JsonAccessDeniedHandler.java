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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde 403 em JSON: o cliente esta autenticado, mas nao tem permissao.
 *
 * <p>401 e 403 nao sao intercambiaveis. 401 significa "nao sei quem voce e"; 403,
 * "sei quem voce e e voce nao pode". Um front-end usa essa diferenca para decidir entre
 * redirecionar ao login ou mostrar "acesso negado".
 */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErroResponse corpo =
                ErroResponse.de(ErrorCode.ACESSO_NEGADO, request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
