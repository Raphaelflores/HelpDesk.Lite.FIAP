package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import br.com.helpdesk.repository.ChamadoRepository;
import br.com.helpdesk.service.mapper.CategoriaMapper;
import br.com.helpdesk.service.mapper.ChamadoMapper;
import br.com.helpdesk.service.mapper.ComentarioMapper;
import br.com.helpdesk.service.mapper.UsuarioMapper;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.ChamadoBuilder;
import br.com.helpdesk.support.Fixtures;
import br.com.helpdesk.support.UsuarioBuilder;
import br.com.helpdesk.web.dto.response.ChamadoResumoResponse;
import br.com.helpdesk.web.dto.response.DashboardResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private MetricasNegocio metricas;

    private DashboardService dashboardService;

    private final Usuario ana = UsuarioBuilder.solicitante().comId(1L).build();
    private final Usuario bruno = UsuarioBuilder.atendente().comId(2L).build();

    @BeforeEach
    void prepararServico() {
        SlaCalculator slaCalculator = new SlaCalculator(Fixtures.CLOCK_FIXO);
        UsuarioMapper usuarioMapper = new UsuarioMapper();
        ChamadoMapper chamadoMapper = new ChamadoMapper(new CategoriaMapper(), usuarioMapper,
                new ComentarioMapper(usuarioMapper), slaCalculator);

        dashboardService = new DashboardService(chamadoRepository,
                new PermissaoService(new TransicaoStatus()), slaCalculator, chamadoMapper, metricas);
    }

    /** Base de 4 chamados: 1 aberto no prazo, 1 aberto atrasado, 1 em atendimento atrasado, 1 resolvido. */
    private List<Chamado> baseDeChamados() {
        return List.of(
                ChamadoBuilder.aberto().comId(1L).comPrioridade(Prioridade.ALTA)
                        .comCategoria(CategoriaBuilder.comSla(8).comId(1L).build())
                        .criadoEm(Fixtures.horasAtras(1)).build(),
                ChamadoBuilder.aberto().comId(2L).comPrioridade(Prioridade.MEDIA)
                        .comCategoria(CategoriaBuilder.comSla(8).comId(1L).build())
                        .criadoEm(Fixtures.horasAtras(30)).build(),
                ChamadoBuilder.emAtendimento().comId(3L).comPrioridade(Prioridade.BAIXA)
                        .comCategoria(CategoriaBuilder.comSla(4).comId(2L).build())
                        .criadoEm(Fixtures.horasAtras(100)).build(),
                ChamadoBuilder.resolvido().comId(4L).comPrioridade(Prioridade.ALTA)
                        .comCategoria(CategoriaBuilder.comSla(48).comId(3L).build())
                        .criadoEm(Fixtures.horasAtras(10))
                        .resolvidoEm(Fixtures.horasAtras(6)).build());
    }

    @Test
    void deveNegarODashboardParaOSolicitante() {
        assertThatThrownBy(() -> dashboardService.resumo(ana))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("todos os status aparecem no mapa, inclusive os zerados")
    void deveContarPorStatusComTodasAsChavesPresentes() {
        when(chamadoRepository.buscarTodosParaDashboard()).thenReturn(baseDeChamados());

        DashboardResumoResponse resumo = dashboardService.resumo(bruno);

        assertThat(resumo.totalPorStatus())
                .containsOnlyKeys(StatusChamado.values())
                .containsEntry(StatusChamado.ABERTO, 2L)
                .containsEntry(StatusChamado.EM_ATENDIMENTO, 1L)
                .containsEntry(StatusChamado.RESOLVIDO, 1L)
                .containsEntry(StatusChamado.FECHADO, 0L);
    }

    @Test
    void deveContarPorPrioridadeComTodasAsChavesPresentes() {
        when(chamadoRepository.buscarTodosParaDashboard()).thenReturn(baseDeChamados());

        DashboardResumoResponse resumo = dashboardService.resumo(bruno);

        assertThat(resumo.totalPorPrioridade())
                .containsOnlyKeys(Prioridade.values())
                .containsEntry(Prioridade.ALTA, 2L)
                .containsEntry(Prioridade.MEDIA, 1L)
                .containsEntry(Prioridade.BAIXA, 1L);
    }

    @Test
    void deveListarOsChamadosForaDoSlaDoMaisAtrasadoParaOMenos() {
        when(chamadoRepository.buscarTodosParaDashboard()).thenReturn(baseDeChamados());

        DashboardResumoResponse resumo = dashboardService.resumo(bruno);

        assertThat(resumo.foraDoSla()).isEqualTo(2);
        assertThat(resumo.chamadosForaDoSla())
                .extracting(ChamadoResumoResponse::id)
                .containsExactly(3L, 2L);   // 96h de atraso, depois 22h
        assertThat(resumo.chamadosForaDoSla()).allMatch(ChamadoResumoResponse::foraDoSla);
    }

    @Test
    void deveCalcularOTotalEOTempoMedioDeResolucao() {
        when(chamadoRepository.buscarTodosParaDashboard()).thenReturn(baseDeChamados());

        DashboardResumoResponse resumo = dashboardService.resumo(bruno);

        assertThat(resumo.totalChamados()).isEqualTo(4);
        assertThat(resumo.tempoMedioResolucaoHoras()).isCloseTo(4d, within(0.001));
    }

    @Test
    void deveDevolverResumoZeradoSemNenhumChamado() {
        when(chamadoRepository.buscarTodosParaDashboard()).thenReturn(List.of());

        DashboardResumoResponse resumo = dashboardService.resumo(bruno);

        assertThat(resumo.totalChamados()).isZero();
        assertThat(resumo.foraDoSla()).isZero();
        assertThat(resumo.tempoMedioResolucaoHoras()).isZero();
        assertThat(resumo.chamadosForaDoSla()).isEmpty();
        assertThat(resumo.totalPorStatus()).containsOnlyKeys(StatusChamado.values());
    }

    @Test
    @DisplayName("cada leitura do dashboard republica os gauges no Micrometer (ADR-018)")
    void deveAtualizarOsGaugesACadaLeitura() {
        when(chamadoRepository.buscarTodosParaDashboard()).thenReturn(baseDeChamados());

        dashboardService.resumo(bruno);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<StatusChamado, Long>> capturado = ArgumentCaptor.forClass(Map.class);
        verify(metricas).atualizarGauges(capturado.capture(), eq(2L));
        assertThat(capturado.getValue()).containsEntry(StatusChamado.ABERTO, 2L);
    }
}
