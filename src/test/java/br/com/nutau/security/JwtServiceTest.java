package br.com.nutau.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.nutau.models.entities.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SEGREDO =
            "bnV0YXUtY2hhdmUtZGUtdGVzdGUtY29tLW1haXMtZGUtMzItYnl0ZXMtcGFyYS1obWFj";
    private static final String EMISSOR = "nutau-api";

    private JwtService jwtService;
    private UsuarioAutenticado usuario;
    private UUID usuarioId;

    @BeforeEach
    void preparar() {
        jwtService = new JwtService(SEGREDO, 120, EMISSOR);

        usuarioId = UUID.randomUUID();
        Usuario entidade = Usuario.builder()
                .id(usuarioId)
                .nome("Maria Souza")
                .email("maria@exemplo.com")
                .cpf("12345678901")
                .senha("$2a$12$hashfalso")
                .build();
        usuario = new UsuarioAutenticado(entidade);
    }

    @Test
    @DisplayName("extrairUsuarioId deve devolver o id quando o token foi emitido por nos")
    void extrairUsuarioId_deveDevolverId_quandoTokenValido() {
        String token = jwtService.gerarToken(usuario);

        assertThat(jwtService.extrairUsuarioId(token)).contains(usuarioId);
    }

    @Test
    @DisplayName("extrairUsuarioId deve devolver vazio quando a assinatura foi adulterada")
    void extrairUsuarioId_deveDevolverVazio_quandoAssinaturaAdulterada() {
        String token = jwtService.gerarToken(usuario);
        // Troca o ultimo caractere: o payload continua legivel, a assinatura nao fecha.
        String adulterado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.extrairUsuarioId(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("extrairUsuarioId deve devolver vazio quando o token expirou")
    void extrairUsuarioId_deveDevolverVazio_quandoExpirado() {
        JwtService jaExpirado = new JwtService(SEGREDO, -1, EMISSOR);

        String token = jaExpirado.gerarToken(usuario);

        assertThat(jaExpirado.extrairUsuarioId(token)).isEmpty();
    }

    @Test
    @DisplayName("extrairUsuarioId deve devolver vazio quando o token foi assinado por outro segredo")
    void extrairUsuarioId_deveDevolverVazio_quandoSegredoDiferente() {
        JwtService outroEmissor = new JwtService(
                "b3V0cm8tc2VncmVkby1jb20tbWFpcy1kZS0zMi1ieXRlcy1wYXJhLWhtYWMtc2hh", 120, EMISSOR);

        String tokenEstrangeiro = outroEmissor.gerarToken(usuario);

        assertThat(jwtService.extrairUsuarioId(tokenEstrangeiro)).isEmpty();
    }

    @Test
    @DisplayName("gerarToken nao deve incluir o CPF no payload")
    void gerarToken_naoDeveIncluirCpf_nunca() {
        // O JWT e apenas assinado, nao criptografado: qualquer um le o payload em Base64.
        // Dado pessoal nao entra ali.
        String token = jwtService.gerarToken(usuario);
        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]));

        assertThat(payload).doesNotContain("12345678901");
        assertThat(payload).contains(usuarioId.toString());
    }

    @Test
    @DisplayName("extrairUsuarioId deve devolver vazio quando o token nao e um JWT")
    void extrairUsuarioId_deveDevolverVazio_quandoTokenMalformado() {
        assertThat(jwtService.extrairUsuarioId("isto-nao-e-um-token")).isEmpty();
    }

    @Test
    @DisplayName("UsuarioAutenticado deve expor o CPF como username")
    void usuarioAutenticado_deveExporCpfComoUsername() {
        Usuario entidade = mock(Usuario.class);
        when(entidade.getId()).thenReturn(usuarioId);
        when(entidade.getNome()).thenReturn("Maria");
        when(entidade.getCpf()).thenReturn("99988877766");
        when(entidade.getSenha()).thenReturn("hash");

        assertThat(new UsuarioAutenticado(entidade).getUsername()).isEqualTo("99988877766");
    }
}
