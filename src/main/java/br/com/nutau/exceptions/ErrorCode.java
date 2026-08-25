package br.com.nutau.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Catalogo unico de erros de negocio da aplicacao.
 *
 * <p>Cada constante amarra tres coisas que antes viviam espalhadas: o status HTTP
 * correspondente, a mensagem exibida ao cliente e se a falha faz sentido ser retentada.
 * O nome da constante e devolvido no corpo da resposta como codigo estavel - o front-end
 * passa a tratar {@code CREDENCIAIS_INVALIDAS} em vez de comparar strings de mensagem,
 * que mudam com qualquer ajuste de texto.
 *
 * <p>O campo {@code retentavel} separa as duas naturezas de falha do sistema. Uma
 * negativa de credito e resposta definitiva: retentar so repete o mesmo resultado. Um
 * bureau fora do ar e falha transitoria: retentar e exatamente o que se deve fazer. Na
 * camada de mensageria e essa distincao que decide entre reprocessar a mensagem ou
 * mandar direto para a Dead Letter Queue.
 */
public enum ErrorCode {

    // --- 400 -----------------------------------------------------------------
    CAMPOS_INVALIDOS(HttpStatus.BAD_REQUEST, "Um ou mais campos estao invalidos"),
    REQUISICAO_INVALIDA(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido ou malformado"),
    PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST, "Parametro invalido"),

    // --- 401 -----------------------------------------------------------------
    /** CPF inexistente ou senha incorreta - deliberadamente indistinguiveis. */
    CREDENCIAIS_INVALIDAS(HttpStatus.UNAUTHORIZED, "CPF ou senha invalidos"),
    NAO_AUTENTICADO(
            HttpStatus.UNAUTHORIZED,
            "Autenticacao necessaria. Envie o header Authorization: Bearer <token>."),

    // --- 403 -----------------------------------------------------------------
    ACESSO_NEGADO(HttpStatus.FORBIDDEN, "Voce nao tem permissao para acessar este recurso."),

    // --- 404 -----------------------------------------------------------------
    USUARIO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Usuario nao encontrado"),
    SOLICITACAO_NAO_ENCONTRADA(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada"),
    CONSULTA_NAO_ENCONTRADA(HttpStatus.NOT_FOUND, "Consulta ao bureau nao encontrada"),
    /** O CEP nao existe na base dos Correios, ou foi recusado pelo ViaCEP. */
    CEP_NAO_ENCONTRADO(
            HttpStatus.NOT_FOUND,
            "CEP nao encontrado. Confira o numero digitado."),
    CARTAO_NAO_ENCONTRADO(
            HttpStatus.NOT_FOUND,
            "Voce ainda nao possui cartao. Solicite a emissao antes de pedir aumento."),

    // --- 409 -----------------------------------------------------------------
    CPF_JA_CADASTRADO(HttpStatus.CONFLICT, "Ja existe uma conta cadastrada com este CPF"),
    EMAIL_JA_CADASTRADO(HttpStatus.CONFLICT, "Ja existe uma conta cadastrada com este e-mail"),
    CONTA_DUPLICADA(HttpStatus.CONFLICT, "Ja existe uma conta com este CPF ou e-mail"),
    SOLICITACAO_JA_FINALIZADA(
            HttpStatus.CONFLICT, "Esta solicitacao ja foi analisada e nao pode ser alterada"),
    /** Impede que N requisicoes simultaneas gerem N cartoes para o mesmo cliente. */
    SOLICITACAO_EM_ANDAMENTO(
            HttpStatus.CONFLICT,
            "Voce ja possui uma solicitacao em analise. Aguarde o resultado."),
    CARTAO_JA_EMITIDO(
            HttpStatus.CONFLICT,
            "Voce ja possui um cartao. Solicite aumento de limite em vez de nova emissao."),
    CARTAO_INATIVO(
            HttpStatus.CONFLICT, "Seu cartao nao esta ativo e nao aceita aumento de limite"),
    VALOR_ABAIXO_DO_LIMITE_ATUAL(
            HttpStatus.CONFLICT, "O valor pedido deve ser maior que o seu limite atual"),
    LIMITE_MAXIMO_ATINGIDO(
            HttpStatus.CONFLICT,
            "Seu limite ja esta no maximo permitido para a renda declarada"),

    // --- 503 (transitorios: valem retentativa) --------------------------------
    BUREAU_INDISPONIVEL(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Servico de consulta de credito temporariamente indisponivel",
            true),
    /**
     * O ViaCEP nao respondeu.
     *
     * <p>Distinto de {@code CEP_NAO_ENCONTRADO} de proposito: aqui nada indica que o CEP
     * seja invalido - a consulta e que falhou. Por isso 503 e retentavel, enquanto o
     * outro e 404 definitivo.
     */
    CEP_SERVICO_INDISPONIVEL(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Nao foi possivel consultar o CEP no momento. Tente novamente em instantes.",
            true),
    FALHA_ENVIO_EMAIL(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Nao foi possivel enviar o e-mail de notificacao",
            true),

    // --- 500 -----------------------------------------------------------------
    ERRO_INTERNO(
            HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");

    private final HttpStatus httpStatus;
    private final String mensagem;
    private final boolean retentavel;

    ErrorCode(HttpStatus httpStatus, String mensagem) {
        this(httpStatus, mensagem, false);
    }

    ErrorCode(HttpStatus httpStatus, String mensagem, boolean retentavel) {
        this.httpStatus = httpStatus;
        this.mensagem = mensagem;
        this.retentavel = retentavel;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMensagem() {
        return mensagem;
    }

    /** Indica se a falha e transitoria e vale a pena tentar de novo. */
    public boolean isRetentavel() {
        return retentavel;
    }
}
