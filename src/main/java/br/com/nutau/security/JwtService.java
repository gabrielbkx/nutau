package br.com.nutau.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emissao e verificacao dos tokens JWT.
 *
 * <p>O token e assinado com HMAC-SHA256, nao criptografado: qualquer pessoa consegue ler
 * o conteudo, mas ninguem consegue alterar sem invalidar a assinatura. Por isso o payload
 * carrega apenas o identificador interno do usuario - nunca CPF, e-mail ou qualquer dado
 * pessoal.
 */
@Slf4j
@Service
public class JwtService {

    /** Nome do usuario, usado apenas para exibicao no front. */
    private static final String CLAIM_NOME = "nome";

    private final SecretKey chave;
    private final long expiracaoMinutos;
    private final String emissor;

    public JwtService(
            @Value("${nutau.jwt.secret}") String secret,
            @Value("${nutau.jwt.expiracao-minutos}") long expiracaoMinutos,
            @Value("${nutau.jwt.emissor}") String emissor) {
        this.chave = construirChave(secret);
        this.expiracaoMinutos = expiracaoMinutos;
        this.emissor = emissor;
    }

    /**
     * Aceita a chave em Base64 ou em texto puro.
     *
     * <p>HMAC-SHA256 exige no minimo 256 bits (32 bytes). Uma chave menor faz o jjwt
     * lancar excecao na inicializacao - falhar aqui, no startup, e infinitamente melhor
     * do que descobrir em producao que os tokens sao trivialmente forjaveis.
     */
    private static SecretKey construirChave(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException naoEhBase64) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /** Gera um token para o usuario autenticado. */
    public String gerarToken(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(expiracaoMinutos, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .issuer(emissor)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .claim(CLAIM_NOME, usuario.getNome())
                .signWith(chave)
                .compact();
    }

    /** Instante de expiracao de um token recem-emitido. */
    public OffsetDateTime calcularExpiracao() {
        return OffsetDateTime.now(ZoneId.systemDefault()).plusMinutes(expiracaoMinutos);
    }

    /**
     * Devolve o ID do usuario se o token for valido, ou {@code Optional.empty()} caso
     * contrario.
     *
     * <p>Nao lanca excecao de proposito: token expirado ou adulterado e situacao rotineira
     * numa API publica, nao evento excepcional. O filtro apenas deixa a requisicao seguir
     * sem autenticacao, e o Spring Security devolve 401.
     */
    public Optional<UUID> extrairUsuarioId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(emissor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT rejeitado: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
