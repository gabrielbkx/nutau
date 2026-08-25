package br.com.nutau.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Endereco residencial do cliente.
 *
 * <p>{@code @Embeddable}, nao {@code @Entity}: o endereco nao tem identidade propria nem
 * existe sem o cliente, e nao e compartilhado entre clientes. As colunas moram na propria
 * tabela {@code usuarios} - sem tabela extra, sem chave estrangeira e sem join para ler o
 * cadastro completo. Uma entidade separada so se justificaria se um cliente pudesse ter
 * varios enderecos ou se o endereco precisasse ser referenciado de fora.
 *
 * <p>Dos sete campos, o cliente digita dois: CEP e numero. Os demais chegam preenchidos
 * pelo ViaCEP.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    /** Somente digitos, sem hifen. A normalizacao acontece no service. */
    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "logradouro", length = 150)
    private String logradouro;

    /** Informado pelo cliente - o ViaCEP nao conhece o numero da casa. */
    @Column(name = "numero", length = 10)
    private String numero;

    /**
     * Opcional, informado pelo cliente.
     *
     * <p>O ViaCEP tambem devolve um campo {@code complemento}, mas com outro significado:
     * la ele qualifica a faixa de numeracao da rua ("lado impar", "ate 500"), nao o
     * apartamento de quem mora. Sobrescrever o que o cliente digitou com aquele texto
     * trocaria "Apto 42" por "lado impar".
     */
    @Column(name = "complemento", length = 60)
    private String complemento;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;
}
