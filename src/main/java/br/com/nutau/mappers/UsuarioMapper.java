package br.com.nutau.mappers;

import br.com.nutau.models.dtos.CadastroRequest;
import br.com.nutau.models.dtos.UsuarioResponse;
import br.com.nutau.models.entities.Usuario;
import br.com.nutau.services.CpfHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Conversao entre {@link Usuario} e seus DTOs.
 *
 * <p>A implementacao e gerada em tempo de compilacao, nao por reflexao: o que roda em
 * producao e codigo Java comum, visivel em {@code target/generated-sources}, sem custo
 * de runtime e com erro de compilacao quando um campo deixa de existir.
 *
 * <p>O build usa {@code unmappedTargetPolicy=ERROR}: todo campo do destino precisa de
 * origem declarada. Acrescentar um campo ao DTO e esquecer de mapea-lo quebra o build
 * em vez de devolver {@code null} silenciosamente na resposta da API.
 */
@Mapper
public interface UsuarioMapper {

    /**
     * Monta a entidade a partir do formulario de cadastro.
     *
     * <p>A senha entra ja em hash, como parametro separado: codificar senha e decisao de
     * seguranca e pertence ao service, nao a uma camada de conversao de tipos. O mapper
     * jamais deve ver a senha em texto puro.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "senha", source = "senhaHash")
    @Mapping(target = "nome", source = "request.nome", qualifiedByName = "aparar")
    @Mapping(target = "email", source = "request.email", qualifiedByName = "normalizarEmail")
    @Mapping(target = "cpf", source = "request.cpf", qualifiedByName = "normalizarCpf")
    @Mapping(target = "rendaMensal", source = "request.rendaMensal")
    Usuario toEntity(CadastroRequest request, String senhaHash);

    /** Representacao publica do cliente. A senha nao existe no destino, logo nunca vaza. */
    @Mapping(target = "cpf", source = "cpf", qualifiedByName = "mascararCpf")
    UsuarioResponse toResponse(Usuario usuario);

    @Named("aparar")
    default String aparar(String valor) {
        return valor == null ? null : valor.trim();
    }

    /**
     * E-mail sempre em minusculas.
     *
     * <p>Sem isso, {@code Maria@x.com} e {@code maria@x.com} passariam pela restricao
     * unica como enderecos diferentes.
     */
    @Named("normalizarEmail")
    default String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** CPF gravado apenas com digitos, independente de como veio na requisicao. */
    @Named("normalizarCpf")
    default String normalizarCpf(String cpf) {
        return CpfHelper.normalizar(cpf);
    }

    /** CPF exibido mascarado, revelando apenas os digitos do meio. */
    @Named("mascararCpf")
    default String mascararCpf(String cpf) {
        return CpfHelper.mascarar(cpf);
    }
}
