package br.com.helpdesk.service;

import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.repository.UsuarioRepository;
import br.com.helpdesk.service.mapper.UsuarioMapper;
import br.com.helpdesk.web.dto.request.UsuarioRequest;
import br.com.helpdesk.web.dto.response.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** CRUD de usuarios (Admin) e a lista publica que alimenta o login simulado. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PermissaoService permissaoService;
    private final UsuarioMapper usuarioMapper;

    /** Publico: monta a tela de login simulado. So usuarios ativos. */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarDisponiveisParaLogin() {
        return usuarioRepository.findAllByAtivoTrueOrderByNomeAsc().stream()
                .map(usuarioMapper::paraResponse)
                .toList();
    }

    /** Login simulado: nao ha senha, so a confirmacao de que o usuario existe e esta ativo. */
    @Transactional(readOnly = true)
    public UsuarioResponse autenticar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", usuarioId));
        if (!usuario.isAtivo()) {
            throw new RegraNegocioException("Usuario inativo nao pode entrar no sistema");
        }
        return usuarioMapper.paraResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Usuario usuarioAtual) {
        permissaoService.verificarGestaoUsuarios(usuarioAtual);
        return usuarioRepository.findAllByOrderByNomeAsc().stream()
                .map(usuarioMapper::paraResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest requisicao, Usuario usuarioAtual) {
        permissaoService.verificarGestaoUsuarios(usuarioAtual);

        String email = requisicao.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Ja existe um usuario com o e-mail " + email);
        }

        Usuario salvo = usuarioRepository.save(Usuario.builder()
                .nome(requisicao.nome().trim())
                .email(email)
                .perfil(requisicao.perfil())
                .ativo(true)
                .build());

        log.info("acao=CRIAR_USUARIO usuarioAlvoId={} perfilAlvo={} perfil={}",
                salvo.getId(), salvo.getPerfil(), usuarioAtual.getPerfil());
        return usuarioMapper.paraResponse(salvo);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest requisicao, Usuario usuarioAtual) {
        permissaoService.verificarGestaoUsuarios(usuarioAtual);

        Usuario usuario = buscar(id);
        String email = requisicao.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RegraNegocioException("Ja existe um usuario com o e-mail " + email);
        }

        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(requisicao.perfil());
        Usuario salvo = usuarioRepository.save(usuario);

        log.info("acao=ATUALIZAR_USUARIO usuarioAlvoId={} perfilAlvo={} perfil={}",
                id, salvo.getPerfil(), usuarioAtual.getPerfil());
        return usuarioMapper.paraResponse(salvo);
    }

    /**
     * Soft delete (ADR-009). Desativar a si mesmo e bloqueado: o admin ficaria sem conseguir
     * entrar de volta, e no MVP nao existe outro caminho de recuperacao.
     */
    @Transactional
    public void desativar(Long id, Usuario usuarioAtual) {
        permissaoService.verificarGestaoUsuarios(usuarioAtual);

        if (Objects.equals(id, usuarioAtual.getId())) {
            throw new RegraNegocioException("Voce nao pode desativar o proprio usuario");
        }

        Usuario usuario = buscar(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        log.info("acao=DESATIVAR_USUARIO usuarioAlvoId={} perfil={}", id, usuarioAtual.getPerfil());
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", id));
    }
}
