package br.com.nutau.integrations;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.services.CepHelper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consulta de endereco por CEP no ViaCEP.
 *
 * <p>Diferente do {@link MockBureauService}, aqui a chamada externa e real. Isso traz
 * duas obrigacoes que uma integracao simulada nao tem:
 *
 * <ul>
 *   <li><b>Timeout explicito.</b> Sem ele, o cliente HTTP espera indefinidamente e uma
 *       indisponibilidade do ViaCEP vira uma requisicao de cadastro pendurada, segurando
 *       uma thread do servidor ate o cliente desistir.
 *   <li><b>Separar "nao existe" de "nao consegui perguntar".</b> Um CEP inexistente e
 *       resposta definitiva - o cliente digitou errado e precisa corrigir. O ViaCEP fora
 *       do ar e falha transitoria. Os dois viram {@code ErrorCode} distintos, e so o
 *       segundo e marcado como retentavel.
 * </ul>
 *
 * <p>Detalhe da API que merece atencao: quando o CEP nao existe, o ViaCEP responde
 * <b>HTTP 200</b> com o corpo {@code {"erro": "true"}}. Confiar apenas no status daria
 * um endereco de campos nulos como se fosse sucesso.
 */
@Slf4j
@Service
public class ViaCepClient {

    private final RestClient restClient;

    public ViaCepClient(
            @Value("${nutau.viacep.url}") String url,
            @Value("${nutau.viacep.timeout-conexao-ms}") long timeoutConexaoMs,
            @Value("${nutau.viacep.timeout-leitura-ms}") long timeoutLeituraMs) {

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutConexaoMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutLeituraMs));

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .build();
    }

    /**
     * Busca o endereco correspondente ao CEP.
     *
     * @param cep com ou sem mascara; apenas os digitos sao enviados
     * @throws NutauException {@code CEP_NAO_ENCONTRADO} quando o CEP nao existe,
     *     {@code CEP_SERVICO_INDISPONIVEL} quando a consulta em si falha
     */
    public ViaCepResposta consultar(String cep) {
        String digitos = CepHelper.normalizar(cep);

        log.info("Consultando ViaCEP para o CEP {}", digitos);

        ViaCepResposta resposta;
        try {
            resposta = restClient.get()
                    .uri("/{cep}/json/", digitos)
                    .retrieve()
                    // 400 do ViaCEP significa CEP fora do formato esperado. Para quem
                    // cadastrou, o efeito e o mesmo de um CEP inexistente: corrigir e
                    // tentar de novo. Retentar sozinho nunca resolveria.
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new NutauException(
                                ErrorCode.CEP_NAO_ENCONTRADO,
                                "ViaCEP respondeu " + res.getStatusCode()
                                        + " para o CEP " + digitos);
                    })
                    .body(ViaCepResposta.class);

        } catch (NutauException e) {
            throw e;
        } catch (RestClientException e) {
            // Timeout, DNS, conexao recusada, 5xx: o ViaCEP nao respondeu. Nada indica
            // que o CEP seja invalido, entao a falha e tecnica e retentavel.
            throw new NutauException(
                    ErrorCode.CEP_SERVICO_INDISPONIVEL,
                    "Falha ao consultar o ViaCEP para o CEP " + digitos,
                    e);
        }

        if (resposta == null || resposta.temErro()) {
            throw new NutauException(
                    ErrorCode.CEP_NAO_ENCONTRADO, "ViaCEP nao conhece o CEP " + digitos);
        }

        log.info("ViaCEP respondeu: {} - {}/{}",
                resposta.logradouro(), resposta.localidade(), resposta.uf());

        return resposta;
    }

    /**
     * Corpo devolvido pelo ViaCEP.
     *
     * <p>Somente os campos que o cadastro usa. O {@code ignoreUnknown} nao e otimismo: a
     * resposta traz ibge, gia, ddd, siafi e outros que nao interessam aqui, e sem ele a
     * desserializacao falharia - alem de quebrar no dia em que a API acrescentar um campo.
     *
     * @param erro presente apenas quando o CEP nao existe; vem como a string "true"
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ViaCepResposta(
            String cep,
            String logradouro,
            String bairro,
            String localidade,
            String uf,
            Boolean erro) {

        boolean temErro() {
            return Boolean.TRUE.equals(erro);
        }
    }
}
