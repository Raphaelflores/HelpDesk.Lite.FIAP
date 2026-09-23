package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.exception.TransicaoInvalidaException;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Comentario;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import br.com.helpdesk.repository.CategoriaRepository;
import br.com.helpdesk.repository.ChamadoRepository;
import br.com.helpdesk.repository.ComentarioRepository;
import br.com.helpdesk.service.mapper.CategoriaMapper;
import br.com.helpdesk.service.mapper.ChamadoMapper;
import br.com.helpdesk.service.mapper.ComentarioMapper;
import br.com.helpdesk.service.mapper.UsuarioMapper;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.ChamadoBuilder;
import br.com.helpdesk.support.Fixtures;
import br.com.helpdesk.support.UsuarioBuilder;
import br.com.helpdesk.web.dto.request.NovoChamadoRequest;
import br.com.helpdesk.web.dto.request.NovoComentarioRequest;
import br.com.helpdesk.web.dto.response.ChamadoResponse;
import br.com.helpdesk.web.dto.response.ComentarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestracao do ChamadoService, com repositorios mockados.
 *
 * <p>O {@link PermissaoService} entra como {@code spy} sobre a implementacao real: assim da
 * para verificar a <b>ordem</b> em que ele e a {@link TransicaoStatus} sao chamados, que e
 * uma regra arquitetural em si (permissao antes de transicao).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChamadoService")
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MetricasNegocio metricas;

    private TransicaoStatus transicaoStatus;
    private PermissaoService permissaoService;
    private ChamadoService chamadoService;

    private final Usuario ana = UsuarioBuilder.solicitante().comId(1L).build();
    private final Usuario bruno = UsuarioBuilder.atendente().comId(2L).build();
    private final Usuario carla = UsuarioBuilder.admin().comId(3L).build();

    @BeforeEach
    void prepararServico() {
        transicaoStatus = spy(new TransicaoStatus());
        permissaoService = spy(new PermissaoService(transicaoStatus));

        SlaCalculator slaCalculator = new SlaCalculator(Fixtures.CLOCK_FIXO);
        UsuarioMapper usuarioMapper = new UsuarioMapper();
        ComentarioMapper comentarioMapper = new ComentarioMapper(usuarioMapper);
        ChamadoMapper chamadoMapper = new ChamadoMapper(
                new CategoriaMapper(), usuarioMapper, comentarioMapper, slaCalculator);

        chamadoService = new ChamadoService(chamadoRepository, comentarioRepository,
                categoriaRepository, permissaoService, transicaoStatus, chamadoMapper,
                comentarioMapper, metricas, Fixtures.CLOCK_FIXO);
    }

    /** Devolve o proprio objeto salvo, como faria o JPA num cenario sem id gerado. */
    private void devolverOChamadoSalvo() {
        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Nested
    @DisplayName("abrir")
    class Abrir {

        @Test
        void deveAbrirOChamadoEmAbertoComOsCarimbosDoClock() {
            // given
            Categoria categoria = CategoriaBuilder.comSla(4).comId(7L).build();
            when(categoriaRepository.findById(7L)).thenReturn(Optional.of(categoria));
            devolverOChamadoSalvo();

            NovoChamadoRequest requisicao = new NovoChamadoRequest(
                    "  Notebook nao liga  ", "  A tela fica preta ao ligar.  ", 7L, Prioridade.ALTA);

            // when
            ChamadoResponse resposta = chamadoService.abrir(requisicao, ana);

            // then
            ArgumentCaptor<Chamado> capturado = ArgumentCaptor.forClass(Chamado.class);
            verify(chamadoRepository).save(capturado.capture());
            Chamado salvo = capturado.getValue();

            assertThat(salvo.getStatus()).isEqualTo(StatusChamado.ABERTO);
            assertThat(salvo.getAtendente()).isNull();
            assertThat(salvo.getSolicitante()).isEqualTo(ana);
            assertThat(salvo.getCriadoEm()).isEqualTo(Fixtures.AGORA);
            assertThat(salvo.getAtualizadoEm()).isEqualTo(Fixtures.AGORA);
            assertThat(salvo.getTitulo()).isEqualTo("Notebook nao liga");
            assertThat(salvo.getDescricao()).isEqualTo("A tela fica preta ao ligar.");

            assertThat(resposta.status()).isEqualTo(StatusChamado.ABERTO);
            assertThat(resposta.comentarios()).isEmpty();
            verify(metricas).chamadoAberto(salvo);
        }

        @Test
        void deveRejeitarAberturaEmCategoriaInexistente() {
            when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());
            NovoChamadoRequest requisicao =
                    new NovoChamadoRequest("Titulo valido", "Descricao valida aqui", 99L, Prioridade.BAIXA);

            assertThatThrownBy(() -> chamadoService.abrir(requisicao, ana))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        void deveRejeitarAberturaEmCategoriaInativa() {
            Categoria inativa = CategoriaBuilder.comSla(4).comId(7L).inativa().build();
            when(categoriaRepository.findById(7L)).thenReturn(Optional.of(inativa));
            NovoChamadoRequest requisicao =
                    new NovoChamadoRequest("Titulo valido", "Descricao valida aqui", 7L, Prioridade.BAIXA);

            assertThatThrownBy(() -> chamadoService.abrir(requisicao, ana))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("inativa");
            verify(chamadoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assumir")
    class Assumir {

        @Test
        void deveDefinirOAtendenteEMudarOStatusParaEmAtendimento() {
            // given
            Chamado chamado = ChamadoBuilder.aberto().comId(10L).comSolicitante(ana).semAtendente().build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L)).thenReturn(List.of());
            devolverOChamadoSalvo();

            // when
            ChamadoResponse resposta = chamadoService.assumir(10L, bruno);

            // then
            assertThat(chamado.getAtendente()).isEqualTo(bruno);
            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
            assertThat(chamado.getAtualizadoEm()).isEqualTo(Fixtures.AGORA);
            assertThat(resposta.atendente().id()).isEqualTo(2L);

            verify(metricas).transicaoRegistrada(
                    StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO, bruno.getPerfil());
        }

        @Test
        void deveRejeitarQueOSolicitanteAssuma() {
            Chamado chamado = ChamadoBuilder.aberto().comId(10L).comSolicitante(ana).semAtendente().build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));

            assertThatThrownBy(() -> chamadoService.assumir(10L, ana))
                    .isInstanceOf(AcessoNegadoException.class);
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("assumir um chamado ja assumido cai em 422, nao em 403")
        void deveRejeitarAssumirChamadoJaEmAtendimento() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L).comSolicitante(ana).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));

            assertThatThrownBy(() -> chamadoService.assumir(10L, bruno))
                    .isInstanceOf(TransicaoInvalidaException.class);
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        void deveDevolver404QuandoOChamadoNaoExiste() {
            when(chamadoRepository.findWithRelacionamentosById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chamadoService.assumir(404L, bruno))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("alterar status")
    class AlterarStatus {

        @Test
        @DisplayName("a permissao e checada ANTES da transicao -- inverter vazaria o estado do chamado")
        void deveVerificarPermissaoAntesDaTransicao() {
            // given: transicao invalida (ABERTO -> RESOLVIDO) pedida por quem nem pode resolver
            Chamado chamado = ChamadoBuilder.aberto().comId(10L).comSolicitante(ana).semAtendente().build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));

            // when / then: ganha o 403, nao o 422
            assertThatThrownBy(() -> chamadoService.alterarStatus(10L, StatusChamado.RESOLVIDO, ana))
                    .isInstanceOf(AcessoNegadoException.class);

            verify(transicaoStatus, never()).validar(any(), any(), any());
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        void deveChamarPermissaoEDepoisTransicaoNaOrdem() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L)
                    .comSolicitante(ana).comAtendente(bruno).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L)).thenReturn(List.of());
            devolverOChamadoSalvo();

            chamadoService.alterarStatus(10L, StatusChamado.RESOLVIDO, bruno);

            InOrder ordem = inOrder(permissaoService, transicaoStatus);
            ordem.verify(permissaoService)
                    .verificarAlteracaoStatus(bruno, chamado, StatusChamado.RESOLVIDO);
            ordem.verify(transicaoStatus)
                    .validar(StatusChamado.EM_ATENDIMENTO, StatusChamado.RESOLVIDO, bruno.getPerfil());
        }

        @Test
        void devePreencherResolvidoEmComOClockAoResolver() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L)
                    .comSolicitante(ana).comAtendente(bruno)
                    .criadoEm(Fixtures.horasAtras(3)).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L)).thenReturn(List.of());
            devolverOChamadoSalvo();

            chamadoService.alterarStatus(10L, StatusChamado.RESOLVIDO, bruno);

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.RESOLVIDO);
            assertThat(chamado.getResolvidoEm()).isEqualTo(Fixtures.AGORA);
            assertThat(chamado.getFechadoEm()).isNull();
            assertThat(chamado.getAtualizadoEm()).isEqualTo(Fixtures.AGORA);
            verify(metricas).tempoDeResolucao(any(Chamado.class), any());
        }

        @Test
        void devePreencherFechadoEmSemApagarResolvidoEmAoFechar() {
            Chamado chamado = ChamadoBuilder.resolvido().comId(10L)
                    .comSolicitante(ana).comAtendente(bruno)
                    .criadoEm(Fixtures.horasAtras(5))
                    .resolvidoEm(Fixtures.horasAtras(2)).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L)).thenReturn(List.of());
            devolverOChamadoSalvo();

            chamadoService.alterarStatus(10L, StatusChamado.FECHADO, ana);

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.FECHADO);
            assertThat(chamado.getFechadoEm()).isEqualTo(Fixtures.AGORA);
            assertThat(chamado.getResolvidoEm()).isEqualTo(Fixtures.horasAtras(2));
        }

        @Test
        @DisplayName("reabrir zera resolvidoEm e fechadoEm, mas mantem o atendente")
        void deveLimparOsCarimbosDeConclusaoAoReabrir() {
            Chamado chamado = ChamadoBuilder.resolvido().comId(10L)
                    .comSolicitante(ana).comAtendente(bruno)
                    .resolvidoEm(Fixtures.horasAtras(2)).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L)).thenReturn(List.of());
            devolverOChamadoSalvo();

            chamadoService.alterarStatus(10L, StatusChamado.EM_ATENDIMENTO, ana);

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
            assertThat(chamado.getResolvidoEm()).isNull();
            assertThat(chamado.getFechadoEm()).isNull();
            assertThat(chamado.getAtendente()).isEqualTo(bruno);
        }

        @Test
        void deveRejeitarTransicaoInvalidaMesmoComPermissao() {
            Chamado chamado = ChamadoBuilder.aberto().comId(10L).comSolicitante(ana).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));

            // A Ana pode fechar os proprios chamados (403 nao se aplica),
            // mas ABERTO -> FECHADO nao existe na maquina de estados.
            assertThatThrownBy(() -> chamadoService.alterarStatus(10L, StatusChamado.FECHADO, ana))
                    .isInstanceOf(TransicaoInvalidaException.class);
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        void devePermitirQueOAdminResolvaChamadoDeOutroAtendente() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L)
                    .comSolicitante(ana).comAtendente(bruno).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L)).thenReturn(List.of());
            devolverOChamadoSalvo();

            chamadoService.alterarStatus(10L, StatusChamado.RESOLVIDO, carla);

            assertThat(chamado.getStatus()).isEqualTo(StatusChamado.RESOLVIDO);
        }
    }

    @Nested
    @DisplayName("comentar e ler")
    class ComentarELer {

        @Test
        void deveSalvarOComentarioComAutorEInstanteDoClock() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L).comSolicitante(ana).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.save(any(Comentario.class)))
                    .thenAnswer(invocacao -> invocacao.getArgument(0));

            ComentarioResponse resposta =
                    chamadoService.comentar(10L, new NovoComentarioRequest("  Segue o print  "), ana);

            ArgumentCaptor<Comentario> capturado = ArgumentCaptor.forClass(Comentario.class);
            verify(comentarioRepository).save(capturado.capture());

            assertThat(capturado.getValue().getTexto()).isEqualTo("Segue o print");
            assertThat(capturado.getValue().getAutor()).isEqualTo(ana);
            assertThat(capturado.getValue().getCriadoEm()).isEqualTo(Fixtures.AGORA);
            assertThat(resposta.texto()).isEqualTo("Segue o print");
        }

        @Test
        void deveNegarComentarioDeSolicitanteEmChamadoAlheio() {
            Usuario outro = UsuarioBuilder.solicitante().comId(9L).build();
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L).comSolicitante(ana).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));

            assertThatThrownBy(() ->
                    chamadoService.comentar(10L, new NovoComentarioRequest("Oi"), outro))
                    .isInstanceOf(AcessoNegadoException.class);
            verify(comentarioRepository, never()).save(any());
        }

        @Test
        void deveNegarLeituraDeDetalheDeChamadoAlheioParaSolicitante() {
            Usuario outro = UsuarioBuilder.solicitante().comId(9L).build();
            Chamado chamado = ChamadoBuilder.aberto().comId(10L).comSolicitante(ana).build();
            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));

            assertThatThrownBy(() -> chamadoService.buscarDetalhe(10L, outro))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void deveDevolverODetalheComOsComentariosDoChamado() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L).comSolicitante(ana).build();
            Comentario comentario = Comentario.builder()
                    .id(1L).chamado(chamado).autor(bruno)
                    .texto("Investigando").criadoEm(Fixtures.horasAtras(1)).build();

            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L))
                    .thenReturn(List.of(comentario));

            ChamadoResponse resposta = chamadoService.buscarDetalhe(10L, ana);

            assertThat(resposta.comentarios()).hasSize(1);
            assertThat(resposta.comentarios().get(0).texto()).isEqualTo("Investigando");
            assertThat(resposta.comentarios().get(0).autor().nome()).isEqualTo(bruno.getNome());
        }

        @Test
        void deveListarOsComentariosDeUmChamadoVisivel() {
            Chamado chamado = ChamadoBuilder.emAtendimento().comId(10L).comSolicitante(ana).build();
            Comentario comentario = Comentario.builder()
                    .id(1L).chamado(chamado).autor(ana)
                    .texto("Alguma novidade?").criadoEm(Fixtures.horasAtras(1)).build();

            when(chamadoRepository.findWithRelacionamentosById(10L)).thenReturn(Optional.of(chamado));
            when(comentarioRepository.findAllByChamadoIdOrderByCriadoEmAsc(10L))
                    .thenReturn(List.of(comentario));

            List<ComentarioResponse> comentarios = chamadoService.listarComentarios(10L, bruno);

            assertThat(comentarios).singleElement()
                    .extracting(ComentarioResponse::texto)
                    .isEqualTo("Alguma novidade?");
        }
    }
}
