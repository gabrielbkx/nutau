package br.com.nutau.services;

import br.com.nutau.exceptions.ErrorCode;
import br.com.nutau.exceptions.NutauException;
import br.com.nutau.mappers.UsuarioMapper;
import br.com.nutau.models.dtos.CadastroRequest;
import br.com.nutau.models.entities.Usuario;
import br.com.nutau.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e consulta de clientes (US01). */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    /**
     * Cria um novo cliente.
     *
     * <p>O CPF e normalizado para somente digitos antes de qualquer coisa. Guardar ora
     * com mascara, ora sem, e o caminho mais curto para o mesmo CPF entrar duas vezes
     * no banco e a restricao unica nao servir para nada.
     */
    @Transactional
    public Usuario cadastrar(CadastroRequest request) {
        String cpf = CpfHelper.normalizar(request.cpf());
        String email = request.email().trim().toLowerCase();

        // Checagem antecipada: existe para produzir uma mensagem de erro legivel.
        // Ela NAO e a garantia de unicidade - duas requisicoes simultaneas passam pelas
        // duas verificacoes antes de qualquer INSERT. Quem garante e a constraint no
        // banco, tratada no catch abaixo.
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new NutauException(ErrorCode.CPF_JA_CADASTRADO);
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new NutauException(ErrorCode.EMAIL_JA_CADASTRADO);
        }

        // O hash e calculado aqui, nao no mapper: codificar senha e decisao de
        // seguranca, e o mapper jamais deve ver a senha em texto puro.
        Usuario usuario =
                usuarioMapper.toEntity(request, passwordEncoder.encode(request.senha()));

        try {
            Usuario salvo = usuarioRepository.saveAndFlush(usuario);
            log.info("Novo cliente cadastrado: id={}", salvo.getId());
            return salvo;
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacaoDeIntegridade(e);
        }
    }

    /**
     * Converte uma violacao de integridade no erro de negocio correspondente.
     *
     * <p>Tratar toda {@code DataIntegrityViolationException} como duplicidade e tentador e
     * perigoso: um NOT NULL violado por bug de mapeamento chega aqui igualzinho a um CPF
     * repetido, e o cliente receberia "conta ja cadastrada" para um defeito do servidor -
     * mensagem errada, status errado e, pior, o bug fica invisivel no log.
     *
     * <p>Por isso a decisao usa o tipo de constraint informado pelo Hibernate. O que nao
     * for violacao de unicidade conhecida vira erro interno e sobe com stack trace.
     */
    private NutauException traduzirViolacaoDeIntegridade(DataIntegrityViolationException e) {
        ConstraintViolationException violacao = extrairViolacao(e);

        if (violacao != null
                && violacao.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {

            String constraint = String.valueOf(violacao.getConstraintName()).toLowerCase();
            log.warn("Cadastro duplicado barrado pela constraint {}", constraint);

            if (constraint.contains("cpf")) {
                return new NutauException(ErrorCode.CPF_JA_CADASTRADO);
            }
            if (constraint.contains("email")) {
                return new NutauException(ErrorCode.EMAIL_JA_CADASTRADO);
            }
            return new NutauException(ErrorCode.CONTA_DUPLICADA);
        }

        return new NutauException(
                ErrorCode.ERRO_INTERNO,
                "Violacao de integridade nao esperada ao cadastrar cliente",
                e);
    }

    private ConstraintViolationException extrairViolacao(Throwable e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao) {
                return violacao;
            }
        }
        return null;
    }

    /** Carrega um cliente pelo identificador interno. */
    @Transactional(readOnly = true)
    public Usuario buscarPorId(java.util.UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NutauException(ErrorCode.USUARIO_NAO_ENCONTRADO));
    }
}
