package br.com.nutau.services;

/**
 * Normalizacao e formatacao de CEP.
 *
 * <p>Mesma divisao de trabalho do {@link CpfHelper}: o formato aceito na entrada e
 * validado de forma declarativa no DTO, e a conversao entre "como o cliente digitou" e
 * "como o banco guarda" mora aqui. O CEP e persistido somente com digitos - guardar ora
 * com hifen, ora sem, e o caminho mais curto para duas representacoes do mesmo endereco.
 */
public final class CepHelper {

    private CepHelper() {
        throw new AssertionError("Classe utilitaria nao deve ser instanciada");
    }

    /** Remove tudo que nao for digito. */
    public static String normalizar(String cep) {
        return cep == null ? null : cep.replaceAll("\\D", "");
    }

    /** Devolve o CEP no formato 00000-000, ou o proprio valor se nao tiver 8 digitos. */
    public static String formatar(String cep) {
        String digitos = normalizar(cep);

        if (digitos == null || digitos.length() != 8) {
            return cep;
        }

        return digitos.substring(0, 5) + "-" + digitos.substring(5);
    }
}
