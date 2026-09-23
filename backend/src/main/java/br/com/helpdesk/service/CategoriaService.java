package br.com.helpdesk.service;

import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.repository.CategoriaRepository;
import br.com.helpdesk.repository.ChamadoRepository;
import br.com.helpdesk.service.mapper.CategoriaMapper;
import br.com.helpdesk.web.dto.request.CategoriaRequest;
import br.com.helpdesk.web.dto.response.CategoriaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de categorias. A escrita e exclusiva do Admin; a leitura e aberta a todos. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ChamadoRepository chamadoRepository;
    private final PermissaoService permissaoService;
    private final CategoriaMapper categoriaMapper;

    /** Somente as ativas: e a lista que alimenta o formulario de novo chamado. */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findAllByAtivaTrueOrderByNomeAsc().stream()
                .map(categoriaMapper::paraResponse)
                .toList();
    }

    /** Inclui as inativas -- a tela de administracao precisa ve-las e poder reativa-las. */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas(Usuario usuarioAtual) {
        permissaoService.verificarGestaoCategorias(usuarioAtual);
        return categoriaRepository.findAllByOrderByNomeAsc().stream()
                .map(categoriaMapper::paraResponse)
                .toList();
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest requisicao, Usuario usuarioAtual) {
        permissaoService.verificarGestaoCategorias(usuarioAtual);

        String nome = requisicao.nome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Ja existe uma categoria chamada " + nome);
        }

        Categoria salva = categoriaRepository.save(Categoria.builder()
                .nome(nome)
                .slaHoras(requisicao.slaHoras())
                .ativa(true)
                .build());

        log.info("acao=CRIAR_CATEGORIA categoriaId={} perfil={}", salva.getId(), usuarioAtual.getPerfil());
        return categoriaMapper.paraResponse(salva);
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest requisicao, Usuario usuarioAtual) {
        permissaoService.verificarGestaoCategorias(usuarioAtual);

        Categoria categoria = buscar(id);
        String nome = requisicao.nome().trim();
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Ja existe uma categoria chamada " + nome);
        }

        categoria.setNome(nome);
        categoria.setSlaHoras(requisicao.slaHoras());
        Categoria salva = categoriaRepository.save(categoria);

        log.info("acao=ATUALIZAR_CATEGORIA categoriaId={} perfil={}", id, usuarioAtual.getPerfil());
        return categoriaMapper.paraResponse(salva);
    }

    /**
     * Soft delete (ADR-009): a categoria e desativada, nunca removida. Chamados historicos
     * precisam continuar apontando para ela, entao o hard delete nem chega a ser oferecido.
     */
    @Transactional
    public void desativar(Long id, Usuario usuarioAtual) {
        permissaoService.verificarGestaoCategorias(usuarioAtual);

        Categoria categoria = buscar(id);
        categoria.setAtiva(false);
        categoriaRepository.save(categoria);

        boolean temChamados = chamadoRepository.existsByCategoriaId(id);
        log.info("acao=DESATIVAR_CATEGORIA categoriaId={} perfil={} possuiChamados={}",
                id, usuarioAtual.getPerfil(), temChamados);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Categoria", id));
    }
}
