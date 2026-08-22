package br.com.nutau.services;

/**
 * Utilitarios de CPF compartilhados pelos services.
 *
 * <p>Substitui o pacote de validacao que existia so para duas classes. A checagem de
 * formato virou um {@code @Pattern} declarativo nos DTOs - continua produzindo erro 400
 * detalhado por campo, junto com as demais anotacoes - e aqui ficaram apenas as duas
 * transformacoes que varios pontos do sistema precisam.
 *
 * <p>Classe utilitaria: final, sem estado e sem construtor publico. Nao e um bean do
 * Spring porque nao ha nada para injetar nem configurar - transformar string nao depende
 * de contexto.
 */
public final class CpfHelper {

    private CpfHelper() {
        throw new AssertionError("Classe utilitaria nao deve ser instanciada");
    }

    /**
     * Reduz o CPF a somente digitos.
     *
     * <p>Chamado antes de qualquer gravacao ou busca. Guardar ora com mascara, ora sem,
     * e o caminho mais curto para o mesmo CPF entrar duas vezes no banco e a restricao
     * unica nao servir para nada.
     */
    public static String normalizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /**
     * Mascara para exibicao, revelando apenas os digitos do meio.
     *
     * <p>Padrao usado por instituicoes financeiras: o suficiente para o cliente
     * reconhecer o proprio documento, insuficiente para alguem reconstrui-lo.
     */
    public static String mascarar(String cpf) {
        String digitos = normalizar(cpf);
        if (digitos == null || digitos.length() != 11) {
            return cpf;
        }
        return "***." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-**";
    }
}
