package br.com.nutau.controllers;

import br.com.nutau.mappers.CartaoMapper;
import br.com.nutau.mappers.SolicitacaoMapper;
import br.com.nutau.models.dtos.ApiResponse;
import br.com.nutau.models.dtos.AumentoLimiteRequest;
import br.com.nutau.models.dtos.CartaoResponse;
import br.com.nutau.models.dtos.SolicitacaoCreditoResponse;
import br.com.nutau.models.entities.CartaoCredito;
import br.com.nutau.models.entities.Usuario;
import br.com.nutau.security.UsuarioAutenticado;
import br.com.nutau.services.CartaoService;
import br.com.nutau.services.CreditoService;
import br.com.nutau.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

/**
 * Cartao do cliente e os dois tipos de pedido de credito.
 *
 * <p>Emissao e aumento sao rotas distintas de proposito. Um endpoint unico com um campo
 * {@code tipo} tornaria o valor solicitado obrigatorio apenas as vezes - regra que o Bean
 * Validation so expressa de forma tortuosa e que o contrato da API nao consegue exibir.
 * Com rotas separadas, cada DTO carrega exatamente os campos que aquele pedido admite.
 */
@Tag(name = "Cartao", description = "Emissao do cartao e aumento de limite")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/cartoes")
@RequiredArgsConstructor
public class CartaoController {

    private final CartaoService cartaoService;
    private final CreditoService creditoService;
    private final UsuarioService usuarioService;
    private final CartaoMapper cartaoMapper;
    private final SolicitacaoMapper solicitacaoMapper;

    /**
     * Solicita a emissao do primeiro cartao (US03).
     *
     * <p>Sem corpo: o cliente nao escolhe o limite inicial. Responde <b>202 Accepted</b>,
     * nao 201 Created - 201 prometeria que o cartao ja existe, e o que existe neste
     * momento e apenas um pedido em analise, que ainda pode ser negado.
     */
    @Operation(summary = "Solicita a emissao do primeiro cartao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping
    public ApiResponse<SolicitacaoCreditoResponse> solicitarEmissao(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ApiResponse.of(solicitacaoMapper.toResponse(
                creditoService.solicitarEmissao(autenticado.getId())));
    }

    /** Solicita aumento de limite sobre o cartao existente. */
    @Operation(summary = "Solicita aumento de limite")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/aumento-limite")
    public ApiResponse<SolicitacaoCreditoResponse> solicitarAumento(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody AumentoLimiteRequest request) {

        return ApiResponse.of(solicitacaoMapper.toResponse(
                creditoService.solicitarAumento(
                        autenticado.getId(), request.valorSolicitado())));
    }

    /**
     * Cartao, categoria e limite atual do cliente.
     *
     * <p>O teto exibido e recalculado sobre a renda atual, e nao guardado no cartao:
     * limite maximo e uma funcao da renda e da categoria, nao um dado que se congela na
     * emissao. Guardar um valor derivado seria criar uma segunda fonte de verdade.
     */
    @Operation(summary = "Consulta o cartao do cliente")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/meu")
    public ApiResponse<CartaoResponse> consultarMeuCartao(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        Usuario usuario = usuarioService.buscarPorId(autenticado.getId());
        CartaoCredito cartao = cartaoService.buscarDoUsuario(usuario.getId());

        return ApiResponse.of(cartaoMapper.toResponse(
                cartao, cartaoService.calcularTetoAumento(cartao, usuario)));
    }
}
