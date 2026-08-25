package br.com.nutau.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.integrations.ViaCepClient.ViaCepResposta;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do cliente do ViaCEP contra um servidor HTTP de verdade, subido em porta
 * aleatoria pelo proprio teste.
 *
 * <p>Nada e mockado aqui de proposito. O que se quer verificar - desserializacao do JSON,
 * traducao de status HTTP em ErrorCode e comportamento no timeout - so aparece quando ha
 * uma requisicao real trafegando. Um mock do RestClient responderia o que o teste mandasse
 * e nao provaria nenhuma dessas tres coisas.
 */
@DisplayName("ViaCepClient")
class ViaCepClientTest {

    private static final String CORPO_SUCESSO =
            "{"
                    + "\"cep\": \"01001-000\","
                    + "\"logradouro\": \"Praca da Se\","
                    + "\"complemento\": \"lado impar\","
                    + "\"bairro\": \"Se\","
                    + "\"localidade\": \"Sao Paulo\","
                    + "\"uf\": \"SP\","
                    + "\"ibge\": \"3550308\","
                    + "\"ddd\": \"11\""
                    + "}";

    private HttpServer servidor;
    private ViaCepClient client;

    /** Path da ultima requisicao recebida - usado para conferir o que foi enviado. */
    private final AtomicReference<String> ultimoPath = new AtomicReference<>();

    /** Roteiro da resposta: status, corpo e atraso antes de responder. */
    private int status = 200;
    private String corpo = CORPO_SUCESSO;
    private long atrasoMs = 0;

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", troca -> {
            ultimoPath.set(troca.getRequestURI().getPath());

            if (atrasoMs > 0) {
                try {
                    Thread.sleep(atrasoMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
        servidor.start();

        // Timeout de leitura curto: o teste de timeout precisa falhar rapido.
        client = new ViaCepClient(
                "http://localhost:" + servidor.getAddress().getPort(), 1000, 400);
    }

    @AfterEach
    void derrubarServidor() {
        servidor.stop(0);
    }

    @Nested
    @DisplayName("quando o CEP existe")
    class Sucesso {

        @Test
        @DisplayName("devolve os campos que o cadastro precisa")
        void consultar_deveDevolverEndereco_quandoCepExiste() {
            ViaCepResposta resposta = client.consultar("01001-000");

            assertThat(resposta.logradouro()).isEqualTo("Praca da Se");
            assertThat(resposta.bairro()).isEqualTo("Se");
            assertThat(resposta.localidade()).isEqualTo("Sao Paulo");
            assertThat(resposta.uf()).isEqualTo("SP");
        }

        @Test
        @DisplayName("campos desconhecidos da resposta nao quebram a desserializacao")
        void consultar_deveIgnorarCamposNaoMapeados() {
            // ibge e ddd vem no corpo e nao existem no record. Sem ignoreUnknown, esta
            // chamada falharia - e falharia de novo a cada campo novo que a API criasse.
            assertThat(client.consultar("01001000")).isNotNull();
        }

        @Test
        @DisplayName("envia o CEP somente com digitos, mesmo recebendo com hifen")
        void consultar_deveNormalizarCep_antesDeChamar() {
            client.consultar("01001-000");

            assertThat(ultimoPath.get()).isEqualTo("/01001000/json/");
        }
    }

    @Nested
    @DisplayName("quando o CEP nao existe")
    class NaoEncontrado {

        @ParameterizedTest(name = "corpo com erro: {0}")
        @ValueSource(strings = {"{\"erro\": \"true\"}", "{\"erro\": true}"})
        @DisplayName("HTTP 200 com erro no corpo vira CEP_NAO_ENCONTRADO")
        void consultar_deveFalhar_quandoCorpoTrazErro(String corpoComErro) {
            // O ViaCEP responde 200 para CEP inexistente. Olhar so o status daria um
            // endereco de campos nulos como se fosse sucesso.
            corpo = corpoComErro;

            assertThatThrownBy(() -> client.consultar("99999999"))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CEP_NAO_ENCONTRADO);
        }

        @Test
        @DisplayName("HTTP 400 tambem vira CEP_NAO_ENCONTRADO, e nao e retentavel")
        void consultar_deveFalhar_quandoViaCepRecusaOFormato() {
            status = 400;
            corpo = "";

            assertThatThrownBy(() -> client.consultar("00000000"))
                    .isInstanceOf(NutauException.class)
                    .satisfies(e -> {
                        NutauException erro = (NutauException) e;
                        assertThat(erro.getErrorCode()).isEqualTo(ErrorCode.CEP_NAO_ENCONTRADO);
                        assertThat(erro.isRetentavel()).isFalse();
                    });
        }
    }

    @Nested
    @DisplayName("quando o ViaCEP falha")
    class Indisponivel {

        @Test
        @DisplayName("HTTP 500 vira CEP_SERVICO_INDISPONIVEL retentavel")
        void consultar_deveFalhar_quandoViaCepDevolve500() {
            status = 500;
            corpo = "";

            assertThatThrownBy(() -> client.consultar("01001000"))
                    .isInstanceOf(NutauException.class)
                    .satisfies(e -> {
                        NutauException erro = (NutauException) e;
                        assertThat(erro.getErrorCode())
                                .isEqualTo(ErrorCode.CEP_SERVICO_INDISPONIVEL);
                        assertThat(erro.isRetentavel()).isTrue();
                    });
        }

        @Test
        @DisplayName("resposta lenta estoura o timeout de leitura em vez de esperar")
        void consultar_deveFalhar_quandoEstouraOTimeout() {
            atrasoMs = 2000;

            assertThatThrownBy(() -> client.consultar("01001000"))
                    .isInstanceOf(NutauException.class)
                    .extracting(e -> ((NutauException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CEP_SERVICO_INDISPONIVEL);
        }

        @Test
        @DisplayName("o detalhe tecnico nao vaza na mensagem publica")
        void consultar_naoDeveVazarDetalheInterno() {
            status = 500;
            corpo = "";

            assertThatThrownBy(() -> client.consultar("01001000"))
                    .isInstanceOf(NutauException.class)
                    .satisfies(e -> assertThat(((NutauException) e).getMensagemPublica())
                            .doesNotContain("localhost"));
        }
    }
}
