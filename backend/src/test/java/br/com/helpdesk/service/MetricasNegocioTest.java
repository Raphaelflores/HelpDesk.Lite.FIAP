package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.ChamadoBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metricas de negocio contra um {@link SimpleMeterRegistry} de verdade -- os nomes e as
 * tags sao contrato com o Prometheus, entao precisam ser afirmados em algum lugar.
 */
@DisplayName("MetricasNegocio")
class MetricasNegocioTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final MetricasNegocio metricas = new MetricasNegocio(registry);

    @Test
    @DisplayName("os gauges ja existem no startup, valendo zero")
    void devePreRegistrarUmGaugePorStatusMaisOForaDoSla() {
        for (StatusChamado status : StatusChamado.values()) {
            assertThat(registry.get(MetricasNegocio.CHAMADOS_POR_STATUS)
                    .tag("status", status.name()).gauge().value()).isZero();
        }
        assertThat(registry.get(MetricasNegocio.CHAMADOS_FORA_SLA).gauge().value()).isZero();
    }

    @Test
    void deveContarChamadoAbertoComTagsDeCategoriaEPrioridade() {
        Chamado chamado = ChamadoBuilder.aberto()
                .comCategoria(CategoriaBuilder.comSla(4).comNome("TI - Infraestrutura").build())
                .comPrioridade(Prioridade.ALTA)
                .build();

        metricas.chamadoAberto(chamado);
        metricas.chamadoAberto(chamado);

        assertThat(registry.get(MetricasNegocio.CHAMADOS_ABERTOS)
                .tag("categoria", "TI - Infraestrutura")
                .tag("prioridade", "ALTA")
                .counter().count()).isEqualTo(2d);
    }

    @Test
    void deveContarTransicoesComTagsDeOrigemDestinoEPerfil() {
        metricas.transicaoRegistrada(StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO, Perfil.ATENDENTE);

        assertThat(registry.get(MetricasNegocio.CHAMADOS_TRANSICOES)
                .tag("de", "ABERTO")
                .tag("para", "EM_ATENDIMENTO")
                .tag("perfil", "ATENDENTE")
                .counter().count()).isEqualTo(1d);
    }

    @Test
    void deveRegistrarOTempoDeResolucaoPorCategoria() {
        Chamado chamado = ChamadoBuilder.resolvido()
                .comCategoria(CategoriaBuilder.comSla(8).comNome("TI - Sistemas").build())
                .build();

        metricas.tempoDeResolucao(chamado, Duration.ofHours(3));

        assertThat(registry.get(MetricasNegocio.TEMPO_RESOLUCAO)
                .tag("categoria", "TI - Sistemas")
                .timer().totalTime(TimeUnit.SECONDS)).isEqualTo(10800d);
    }

    @Test
    void deveAtualizarOsGaugesComOsValoresDoDashboard() {
        metricas.atualizarGauges(Map.of(
                StatusChamado.ABERTO, 5L,
                StatusChamado.EM_ATENDIMENTO, 2L), 3L);

        assertThat(registry.get(MetricasNegocio.CHAMADOS_POR_STATUS)
                .tag("status", "ABERTO").gauge().value()).isEqualTo(5d);
        assertThat(registry.get(MetricasNegocio.CHAMADOS_POR_STATUS)
                .tag("status", "EM_ATENDIMENTO").gauge().value()).isEqualTo(2d);
        // Status ausente do mapa volta a zero, em vez de manter o valor antigo.
        assertThat(registry.get(MetricasNegocio.CHAMADOS_POR_STATUS)
                .tag("status", "FECHADO").gauge().value()).isZero();
        assertThat(registry.get(MetricasNegocio.CHAMADOS_FORA_SLA).gauge().value()).isEqualTo(3d);
    }
}
