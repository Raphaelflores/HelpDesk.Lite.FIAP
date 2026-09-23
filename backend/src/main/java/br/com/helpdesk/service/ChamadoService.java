package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Comentario;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import br.com.helpdesk.repository.CategoriaRepository;
import br.com.helpdesk.repository.ChamadoRepository;
import br.com.helpdesk.repository.ChamadoSpecification;
import br.com.helpdesk.repository.ComentarioRepository;
import br.com.helpdesk.service.mapper.ChamadoMapper;
import br.com.helpdesk.service.mapper.ComentarioMapper;
import br.com.helpdesk.web.dto.request.NovoChamadoRequest;
import br.com.helpdesk.web.dto.request.NovoComentarioRequest;
import br.com.helpdesk.web.dto.response.ChamadoResponse;
import br.com.helpdesk.web.dto.response.ChamadoResumoResponse;
import br.com.helpdesk.web.dto.response.ComentarioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Casos de uso do chamado.
 *
 * <p>Toda operacao segue a mesma sequencia, nesta ordem (secao 4.6 do documento):</p>
 * <ol>
 *   <li>busca o chamado -- 404 se nao existe;</li>
 *   <li>{@link PermissaoService} -- 403 se o usuario nao pode;</li>
 *   <li>{@link TransicaoStatus} -- 422 se a transicao nao vale agora;</li>
 *   <li>persiste, registra metrica e escreve a linha de auditoria.</li>
 * </ol>
 *
 * <p>Inverter os passos 2 e 3 vazaria o estado do chamado para quem nao pode ve-lo.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ComentarioRepository comentarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final PermissaoService permissaoService;
    private final TransicaoStatus transicaoStatus;
    private final ChamadoMapper chamadoMapper;
    private final ComentarioMapper comentarioMapper;
    private final MetricasNegocio metricas;
    private final Clock clock;

    // ------------------------------------------------------------------ leitura

    /**
     * Lista paginada. A visibilidade do solicitante entra na {@code Specification}, ou seja,
     * no WHERE da query -- nem a pagina nem o count vazam chamados de terceiros.
     */
    @Transactional(readOnly = true)
    public Page<ChamadoResumoResponse> listar(Usuario usuarioAtual,
                                              StatusChamado status,
                                              Prioridade prioridade,
                                              Long categoriaId,
                                              boolean meus,
                                              Pageable paginacao) {
        return chamadoRepository
                .findAll(ChamadoSpecification.comFiltros(usuarioAtual.getPerfil(), usuarioAtual.getId(),
                        status, prioridade, categoriaId, meus), paginacao)
                .map(chamadoMapper::paraResumo);
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscarDetalhe(Long id, Usuario usuarioAtual) {
        Chamado chamado = buscarComPermissaoDeLeitura(id, usuarioAtual);
        return chamadoMapper.paraResponse(chamado, comentariosDe(id));
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> listarComentarios(Long chamadoId, Usuario usuarioAtual) {
        buscarComPermissaoDeLeitura(chamadoId, usuarioAtual);
        return comentariosDe(chamadoId).stream().map(comentarioMapper::paraResponse).toList();
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public ChamadoResponse abrir(NovoChamadoRequest requisicao, Usuario usuarioAtual) {
        permissaoService.verificarAbertura(usuarioAtual);

        Categoria categoria = categoriaRepository.findById(requisicao.categoriaId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Categoria", requisicao.categoriaId()));
        if (!categoria.isAtiva()) {
            throw new RegraNegocioException("A categoria %s esta inativa".formatted(categoria.getNome()));
        }

        Instant agora = Instant.now(clock);
        Chamado chamado = Chamado.builder()
                .titulo(requisicao.titulo().trim())
                .descricao(requisicao.descricao().trim())
                .status(StatusChamado.ABERTO)
                .prioridade(requisicao.prioridade())
                .categoria(categoria)
                .solicitante(usuarioAtual)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();

        Chamado salvo = chamadoRepository.save(chamado);

        metricas.chamadoAberto(salvo);
        auditar("ABRIR_CHAMADO", usuarioAtual, salvo.getId(), null, StatusChamado.ABERTO);

        return chamadoMapper.paraResponse(salvo, List.of());
    }

    /**
     * Assumir: o atendente vira responsavel e o chamado vai para EM_ATENDIMENTO.
     *
     * <p>Assumir um chamado ja assumido cai em 422: ele nao esta mais em ABERTO, e
     * {@code EM_ATENDIMENTO -> EM_ATENDIMENTO} nao existe na maquina de estados.</p>
     */
    @Transactional
    public ChamadoResponse assumir(Long id, Usuario usuarioAtual) {
        Chamado chamado = buscar(id);

        permissaoService.verificarAssuncao(usuarioAtual, chamado);
        StatusChamado statusAnterior = chamado.getStatus();
        transicaoStatus.validar(statusAnterior, StatusChamado.EM_ATENDIMENTO, usuarioAtual.getPerfil());

        chamado.setAtendente(usuarioAtual);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setAtualizadoEm(Instant.now(clock));

        Chamado salvo = chamadoRepository.save(chamado);

        metricas.transicaoRegistrada(statusAnterior, StatusChamado.EM_ATENDIMENTO, usuarioAtual.getPerfil());
        auditar("ASSUMIR_CHAMADO", usuarioAtual, id, statusAnterior, StatusChamado.EM_ATENDIMENTO);

        return chamadoMapper.paraResponse(salvo, comentariosDe(id));
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, StatusChamado novoStatus, Usuario usuarioAtual) {
        Chamado chamado = buscar(id);

        // Ordem obrigatoria: quem pode (403) antes de pode agora (422).
        permissaoService.verificarAlteracaoStatus(usuarioAtual, chamado, novoStatus);
        StatusChamado statusAnterior = chamado.getStatus();
        transicaoStatus.validar(statusAnterior, novoStatus, usuarioAtual.getPerfil());

        aplicarEfeitosDaTransicao(chamado, novoStatus);
        Chamado salvo = chamadoRepository.save(chamado);

        metricas.transicaoRegistrada(statusAnterior, novoStatus, usuarioAtual.getPerfil());
        auditar("ALTERAR_STATUS", usuarioAtual, id, statusAnterior, novoStatus);

        return chamadoMapper.paraResponse(salvo, comentariosDe(id));
    }

    @Transactional
    public ComentarioResponse comentar(Long chamadoId, NovoComentarioRequest requisicao,
                                       Usuario usuarioAtual) {
        Chamado chamado = buscar(chamadoId);
        permissaoService.verificarComentario(usuarioAtual, chamado);

        Comentario comentario = Comentario.builder()
                .chamado(chamado)
                .autor(usuarioAtual)
                .texto(requisicao.texto().trim())
                .criadoEm(Instant.now(clock))
                .build();

        Comentario salvo = comentarioRepository.save(comentario);
        log.info("acao=COMENTAR chamadoId={} comentarioId={} perfil={}",
                chamadoId, salvo.getId(), usuarioAtual.getPerfil());

        return comentarioMapper.paraResponse(salvo);
    }

    // ------------------------------------------------------------------ apoio

    /**
     * Carimbos de tempo de cada transicao. Todos vem do Clock injetado.
     *
     * <p>Reabrir zera resolvidoEm e fechadoEm: o chamado voltou a estar em aberto, e
     * conta-lo como resolvido distorceria o tempo medio e o calculo de SLA. O atendente e
     * mantido, para o chamado voltar para quem ja o conhece.</p>
     */
    private void aplicarEfeitosDaTransicao(Chamado chamado, StatusChamado novoStatus) {
        Instant agora = Instant.now(clock);

        switch (novoStatus) {
            case RESOLVIDO -> {
                chamado.setResolvidoEm(agora);
                metricas.tempoDeResolucao(chamado, Duration.between(chamado.getCriadoEm(), agora));
            }
            case FECHADO -> chamado.setFechadoEm(agora);
            case EM_ATENDIMENTO -> {
                chamado.setResolvidoEm(null);
                chamado.setFechadoEm(null);
            }
            case ABERTO -> {
                // Inalcancavel: nenhuma aresta leva de volta para ABERTO.
            }
        }

        chamado.setStatus(novoStatus);
        chamado.setAtualizadoEm(agora);
    }

    private Chamado buscar(Long id) {
        return chamadoRepository.findWithRelacionamentosById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Chamado", id));
    }

    private Chamado buscarComPermissaoDeLeitura(Long id, Usuario usuarioAtual) {
        Chamado chamado = buscar(id);
        permissaoService.verificarVisualizacao(usuarioAtual, chamado);
        return chamado;
    }

    private List<Comentario> comentariosDe(Long chamadoId) {
        return comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(chamadoId);
    }

    /**
     * Linha de auditoria da secao 4.11. {@code requestId} e {@code usuarioId} nao entram na
     * mensagem: ja vem do MDC no pattern do Logback. Nada de dado pessoal alem do id.
     */
    private void auditar(String acao, Usuario usuario, Long chamadoId,
                         StatusChamado de, StatusChamado para) {
        log.info("acao={} chamadoId={} perfil={} statusDe={} statusPara={}",
                acao, chamadoId, usuario.getPerfil(), de, para);
    }
}
