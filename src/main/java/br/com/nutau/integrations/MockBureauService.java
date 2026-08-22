package br.com.nutau.integrations;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.models.enums.ResultadoBureau;
import br.com.nutau.models.enums.TipoBureau;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Simulador dos bureaus de credito (Serasa e SPC).
 *
 * <p>Nenhuma chamada externa acontece aqui. O resultado e deterministico e vem dos dois
 * ultimos digitos do CPF, o que torna cada cenario reproduzivel em teste:
 *
 * <ul>
 *   <li>{@code ...00} - bureau fora do ar: lanca {@link BureauUnavailableException}
 *       (exercita retentativa e DLQ do RabbitMQ)
 *   <li>{@code ...11} - Serasa reprova por {@code SCORE_BAIXO}; o SPC aprova
 *   <li>{@code ...22} - SPC reprova por {@code NOME_SUJO}; o Serasa aprova
 *   <li>qualquer outro - ambos aprovam
 * </ul>
 *
 * <p>Os casos 11 e 22 sao especificos de cada bureau de proposito: o SPC nao calcula
 * score e o Serasa nao mantem o cadastro de inadimplentes do SPC. Isso deixa visivel a
 * regra central da analise - <b>basta um reprovar para o credito ser negado</b>.
 */
@Slf4j
@Service
public class MockBureauService {

    private static final String SUFIXO_INDISPONIVEL = "00";
    private static final String SUFIXO_SCORE_BAIXO = "11";
    private static final String SUFIXO_NOME_SUJO = "22";

    private final long latenciaMs;

    public MockBureauService(@Value("${nutau.bureau.latencia-ms}") long latenciaMs) {
        this.latenciaMs = latenciaMs;
    }

    /**
     * Consulta um bureau.
     *
     * @throws NutauException com {@code ErrorCode.BUREAU_INDISPONIVEL} quando o bureau
     *     simula estar fora do ar - falha tecnica, retentavel, e nao uma negativa de credito
     */
    public ResultadoBureau consultar(TipoBureau bureau, String cpf) {
        log.info("Consultando {} para o CPF terminado em {}", bureau, sufixo(cpf));

        simularLatenciaDeRede();

        String sufixo = sufixo(cpf);

        if (SUFIXO_INDISPONIVEL.equals(sufixo)) {
            throw new NutauException(
                    ErrorCode.BUREAU_INDISPONIVEL,
                    "Bureau " + bureau + " indisponivel no momento.");
        }

        ResultadoBureau resultado = switch (bureau) {
            case SERASA -> SUFIXO_SCORE_BAIXO.equals(sufixo)
                    ? ResultadoBureau.SCORE_BAIXO
                    : ResultadoBureau.APROVADO;
            case SPC -> SUFIXO_NOME_SUJO.equals(sufixo)
                    ? ResultadoBureau.NOME_SUJO
                    : ResultadoBureau.APROVADO;
        };

        log.info("Bureau {} respondeu: {}", bureau, resultado);
        return resultado;
    }

    /**
     * Bloqueia a thread para imitar a latencia de uma chamada HTTP externa.
     *
     * <p>Bloquear thread e exatamente o que nao se deve fazer num controller - e o motivo
     * de este trabalho rodar num consumidor de fila, e nao dentro da requisicao do cliente.
     */
    private void simularLatenciaDeRede() {
        try {
            Thread.sleep(latenciaMs);
        } catch (InterruptedException e) {
            /*
             * Capturar InterruptedException e seguir em frente apaga o pedido de
             * interrupcao e impede a JVM de encerrar a thread no shutdown. Restaurar
             * o flag e converter em excecao e o unico tratamento correto.
             */
            Thread.currentThread().interrupt();
            throw new NutauException(
                    ErrorCode.BUREAU_INDISPONIVEL, "Consulta ao bureau interrompida", e);
        }
    }

    /** Ultimos dois digitos do CPF, ou string vazia se o CPF for curto demais. */
    private String sufixo(String cpf) {
        if (cpf == null || cpf.length() < 2) {
            return "";
        }
        return cpf.substring(cpf.length() - 2);
    }
}
