package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Chamado;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metricas de negocio da secao 4.11 do documento.
 *
 * <p>Unico ponto do sistema que fala com o {@link MeterRegistry}: nenhum service injeta o
 * registry diretamente. Isso mantem os nomes e as tags das metricas num lugar so.</p>
 *
 * <table>
 *   <caption>Metricas expostas</caption>
 *   <tr><th>Metrica</th><th>Tipo</th><th>Tags</th></tr>
 *   <tr><td>helpdesk_chamados_abertos_total</td><td>Counter</td><td>categoria, prioridade</td></tr>
 *   <tr><td>helpdesk_chamados_transicoes_total</td><td>Counter</td><td>de, para, perfil</td></tr>
 *   <tr><td>helpdesk_chamados_por_status</td><td>Gauge</td><td>status</td></tr>
 *   <tr><td>helpdesk_chamados_fora_sla</td><td>Gauge</td><td>-</td></tr>
 *   <tr><td>helpdesk_tempo_resolucao_segundos</td><td>Timer</td><td>categoria</td></tr>
 * </table>
 */
@Component
public class MetricasNegocio {

    public static final String CHAMADOS_ABERTOS = "helpdesk_chamados_abertos_total";
    public static final String CHAMADOS_TRANSICOES = "helpdesk_chamados_transicoes_total";
    public static final String CHAMADOS_POR_STATUS = "helpdesk_chamados_por_status";
    public static final String CHAMADOS_FORA_SLA = "helpdesk_chamados_fora_sla";
    public static final String TEMPO_RESOLUCAO = "helpdesk_tempo_resolucao_segundos";

    private final MeterRegistry registry;

    /** Holders dos gauges. Pre-registrados no startup para o /prometheus ja listar tudo. */
    private final Map<StatusChamado, AtomicLong> porStatus = new EnumMap<>(StatusChamado.class);
    private final AtomicLong foraDoSla = new AtomicLong(0);

    public MetricasNegocio(MeterRegistry registry) {
        this.registry = registry;

        for (StatusChamado status : StatusChamado.values()) {
            AtomicLong holder = new AtomicLong(0);
            porStatus.put(status, holder);
            Gauge.builder(CHAMADOS_POR_STATUS, holder, AtomicLong::doubleValue)
                    .description("Quantidade de chamados por status")
                    .tag("status", status.name())
                    .register(registry);
        }

        Gauge.builder(CHAMADOS_FORA_SLA, foraDoSla, AtomicLong::doubleValue)
                .description("Quantidade de chamados fora do SLA")
                .register(registry);
    }

    /** Registrado em ChamadoService.abrir. */
    public void chamadoAberto(Chamado chamado) {
        Counter.builder(CHAMADOS_ABERTOS)
                .description("Total de chamados abertos")
                .tags(Tags.of(
                        "categoria", chamado.getCategoria().getNome(),
                        "prioridade", nomeDe(chamado.getPrioridade())))
                .register(registry)
                .increment();
    }

    /** Registrado em ChamadoService.assumir e ChamadoService.alterarStatus. */
    public void transicaoRegistrada(StatusChamado de, StatusChamado para, Perfil perfil) {
        Counter.builder(CHAMADOS_TRANSICOES)
                .description("Total de transicoes de status")
                .tags(Tags.of("de", de.name(), "para", para.name(), "perfil", perfil.name()))
                .register(registry)
                .increment();
    }

    /** Registrado ao entrar em RESOLVIDO: resolvidoEm - criadoEm. */
    public void tempoDeResolucao(Chamado chamado, Duration duracao) {
        Timer.builder(TEMPO_RESOLUCAO)
                .description("Tempo entre a abertura e a resolucao do chamado")
                .tag("categoria", chamado.getCategoria().getNome())
                .register(registry)
                .record(duracao);
    }

    /**
     * Atualiza os gauges. Chamado pelo DashboardService a cada leitura do resumo (ADR-018):
     * sem agendador e sem query disparada pelo scrape do Prometheus.
     */
    public void atualizarGauges(Map<StatusChamado, Long> totalPorStatus, long quantidadeForaDoSla) {
        porStatus.forEach((status, holder) -> holder.set(totalPorStatus.getOrDefault(status, 0L)));
        foraDoSla.set(quantidadeForaDoSla);
    }

    private static String nomeDe(Prioridade prioridade) {
        return prioridade == null ? "DESCONHECIDA" : prioridade.name();
    }
}
