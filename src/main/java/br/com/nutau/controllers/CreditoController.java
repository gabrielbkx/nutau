package br.com.nutau.controllers;

import br.com.nutau.mappers.SolicitacaoMapper;
import br.com.nutau.models.dtos.ApiResponse;
import br.com.nutau.models.dtos.SolicitacaoCreditoResponse;
import br.com.nutau.security.UsuarioAutenticado;
import br.com.nutau.services.CreditoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acompanhamento das solicitacoes de credito.
 *
 * <p>Somente leitura. A abertura de pedidos vive em {@link CartaoController}, junto do
 * recurso sobre o qual a decisao recai.
 */
@Tag(name = "Solicitacoes", description = "Acompanhamento das analises de credito")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
public class CreditoController {

    private final CreditoService creditoService;
    private final SolicitacaoMapper solicitacaoMapper;

    @Operation(summary = "Consulta o andamento de uma solicitacao")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ApiResponse<SolicitacaoCreditoResponse> consultar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PathVariable UUID id) {

        return ApiResponse.of(solicitacaoMapper.toResponse(
                creditoService.buscarDoUsuario(id, autenticado.getId())));
    }

    @Operation(summary = "Historico de solicitacoes do cliente")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ApiResponse<List<SolicitacaoCreditoResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        return ApiResponse.of(solicitacaoMapper.toResponseList(
                creditoService.listarDoUsuario(autenticado.getId())));
    }
}
