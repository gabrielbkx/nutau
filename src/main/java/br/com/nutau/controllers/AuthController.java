package br.com.nutau.controllers;

import br.com.nutau.mappers.UsuarioMapper;
import br.com.nutau.models.dtos.ApiResponse;
import br.com.nutau.models.dtos.CadastroRequest;
import br.com.nutau.models.dtos.LoginRequest;
import br.com.nutau.models.dtos.TokenResponse;
import br.com.nutau.models.dtos.UsuarioResponse;
import br.com.nutau.security.UsuarioAutenticado;
import br.com.nutau.services.AutenticacaoService;
import br.com.nutau.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login e identificacao do cliente autenticado. */
@Tag(name = "Autenticacao", description = "Cadastro e login de clientes")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;
    private final UsuarioMapper usuarioMapper;

    @Operation(summary = "Cadastra um novo cliente (US01)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cadastro")
    public ApiResponse<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        return ApiResponse.of(usuarioMapper.toResponse(usuarioService.cadastrar(request)));
    }

    @Operation(summary = "Autentica e devolve um token JWT (US02)")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(autenticacaoService.autenticar(request));
    }

    /**
     * Dados do cliente autenticado.
     *
     * <p>O {@code @AuthenticationPrincipal} entrega o objeto que o filtro JWT colocou no
     * contexto. O ID do cliente vem sempre dai, nunca do corpo ou da URL: confiar num ID
     * enviado pelo proprio cliente seria autorizar qualquer um a agir como qualquer outro.
     */
    @Operation(summary = "Dados do cliente autenticado")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/eu")
    public ApiResponse<UsuarioResponse> eu(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ApiResponse.of(
                usuarioMapper.toResponse(usuarioService.buscarPorId(autenticado.getId())));
    }
}
