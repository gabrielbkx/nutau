package br.com.nutau.exceptions;

/**
 * Unica excecao de negocio da aplicacao.
 *
 * <p>Toda falha esperada e sinalizada por esta classe, variando apenas o {@link ErrorCode}.
 * Isso elimina a hierarquia de excecoes que cresce sem controle - e com ela a lista de
 * {@code @ExceptionHandler} quase identicos que so mudam o status devolvido.
 *
 * <p>A mensagem padrao vem do proprio {@code ErrorCode}. O construtor com detalhe existe
 * para os casos em que a causa precisa aparecer no log sem alterar o texto exibido ao
 * cliente.
 */
public class NutauException extends RuntimeException {

    private final ErrorCode errorCode;

    public NutauException(ErrorCode errorCode) {
        super(errorCode.getMensagem());
        this.errorCode = errorCode;
    }

    public NutauException(ErrorCode errorCode, String detalhe) {
        super(detalhe);
        this.errorCode = errorCode;
    }

    public NutauException(ErrorCode errorCode, String detalhe, Throwable causa) {
        super(detalhe, causa);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** Mensagem segura para exibir ao cliente - nunca o detalhe interno. */
    public String getMensagemPublica() {
        return errorCode.getMensagem();
    }

    /** Atalho para a camada de mensageria decidir entre retentar e mandar para a DLQ. */
    public boolean isRetentavel() {
        return errorCode.isRetentavel();
    }
}
