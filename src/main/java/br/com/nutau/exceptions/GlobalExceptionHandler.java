package br.com.nutau.exceptions;

import br.com.nutau.models.dtos.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes em respostas HTTP num formato unico.
 *
 * <p>Vive junto de {@link ErrorCode} e {@link NutauException}, e nao entre os controllers:
 * os tres formam uma unidade so - o catalogo de erros, a excecao que o carrega e a
 * traducao para HTTP. Quem for acrescentar um erro novo mexe nos tres no mesmo lugar.
 *
 * <p>Com uma unica excecao de negocio, os handlers daqui se dividem em dois grupos: um
 * que atende toda a {@link NutauException} lendo o status do proprio {@link ErrorCode},
 * e alguns que convertem excecoes do framework - que nao temos como fazer nascer ja
 * com um ErrorCode - no mesmo formato.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Toda falha de negocio do sistema passa por aqui.
     *
     * <p>O status HTTP vem do {@code ErrorCode}, entao acrescentar um erro novo ao
     * catalogo nao exige tocar nesta classe. Note que a resposta usa a mensagem publica
     * do codigo, nunca {@code e.getMessage()}: o detalhe interno vai apenas para o log.
     */
    @ExceptionHandler(NutauException.class)
    public ResponseEntity<ErroResponse> tratarNutau(
            NutauException e, HttpServletRequest request) {

        ErrorCode codigo = e.getErrorCode();

        if (codigo.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", codigo, e.getMessage(), e);
        } else {
            log.info("[{}] {}", codigo, e.getMessage());
        }

        return ResponseEntity
                .status(codigo.getHttpStatus())
                .body(ErroResponse.de(codigo, request.getRequestURI()));
    }

    /** Falha de {@code @Valid}: detalha campo a campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        List<ErroResponse.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoInvalido(
                        erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ErroResponse.deValidacao(request.getRequestURI(), campos));
    }

    /** JSON malformado ou tipo incompativel no corpo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        return responder(ErrorCode.REQUISICAO_INVALIDA, request);
    }

    /** Ex.: um UUID invalido em {@code /api/solicitacoes/{id}}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoInvalido(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        return ResponseEntity
                .status(ErrorCode.PARAMETRO_INVALIDO.getHttpStatus())
                .body(ErroResponse.de(
                        ErrorCode.PARAMETRO_INVALIDO,
                        "Valor invalido para o parametro '" + e.getName() + "'",
                        request.getRequestURI()));
    }

    /**
     * Credenciais invalidas vindas do Spring Security.
     *
     * <p>{@code BadCredentialsException} (senha errada) e {@code UsernameNotFoundException}
     * (CPF inexistente) caem na mesma resposta de proposito. Diferenciar as duas
     * transformaria o login num enumerador de clientes do banco.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(
            RuntimeException e, HttpServletRequest request) {

        log.info("Tentativa de login rejeitada em {}", request.getRequestURI());
        return responder(ErrorCode.CREDENCIAIS_INVALIDAS, request);
    }

    /**
     * Autorizacao negada dentro do controller (por exemplo, via {@code @PreAuthorize}).
     *
     * <p>Sem este handler, a excecao cairia no catch-all abaixo e viraria 500 - um "erro
     * do servidor" para o que na verdade e uma decisao deliberada dele.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(
            AccessDeniedException e, HttpServletRequest request) {

        return responder(ErrorCode.ACESSO_NEGADO, request);
    }

    /**
     * Rede de seguranca para o que nao foi previsto.
     *
     * <p>A causa real vai para o log, nunca para o corpo da resposta: stack trace exposto
     * entrega versao de framework, estrutura de pacotes e, as vezes, trechos de query.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(
            Exception e, HttpServletRequest request) {

        log.error("Erro nao tratado em {} {}", request.getMethod(), request.getRequestURI(), e);
        return responder(ErrorCode.ERRO_INTERNO, request);
    }

    private ResponseEntity<ErroResponse> responder(
            ErrorCode codigo, HttpServletRequest request) {
        return ResponseEntity
                .status(codigo.getHttpStatus())
                .body(ErroResponse.de(codigo, request.getRequestURI()));
    }
}
