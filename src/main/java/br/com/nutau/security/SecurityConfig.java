package br.com.nutau.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Configuracao central do Spring Security. */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Rotas abertas: cadastro, login e ferramentas de inspecao do ambiente local. */
    private static final String[] ROTAS_PUBLICAS = {
        "/api/auth/cadastro",
        "/api/auth/login",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/actuator/health"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    @Value("${nutau.cors.origens-permitidas:http://localhost:3000,http://localhost:5173}")
    private List<String> origensPermitidas;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                /*
                 * CSRF desligado porque a API e stateless e autenticada por header.
                 * O ataque CSRF depende do navegador anexar a credencial sozinho - o que
                 * acontece com cookie de sessao, nao com um header que o JavaScript
                 * precisa montar explicitamente. Desligar CSRF numa API com sessao por
                 * cookie, por outro lado, seria uma falha grave.
                 */
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                /*
                 * STATELESS: o servidor nao guarda sessao nenhuma. Todo o estado de
                 * autenticacao vive no token, no cliente. E isso que permite rodar N
                 * instancias da API atras de um load balancer sem sessao compartilhada.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        // Tudo o que nao foi liberado acima exige token valido.
                        // A ordem importa: a primeira regra que casa e a que vale.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * BCrypt com custo 12.
     *
     * <p>O custo e exponencial: 12 significa 2^12 rodadas, cerca de 250 ms por hash em
     * hardware atual. Essa lentidao e a funcionalidade, nao um defeito - ela torna
     * inviavel testar bilhoes de senhas caso o banco vaze. O salt e gerado por hash e
     * embutido no proprio resultado, por isso nao existe coluna separada para ele.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposto como bean para que o controller de login possa delegar a verificacao de
     * credenciais ao Spring em vez de comparar hashes na mao.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
