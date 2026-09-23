package br.com.helpdesk.web.dto.response;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;

import java.util.List;
import java.util.Map;

/**
 * Indicadores do dashboard (secao 8.3 do documento).
 *
 * @param totalPorStatus            todos os status presentes, inclusive os zerados
 * @param totalPorPrioridade        todas as prioridades presentes, inclusive as zeradas
 * @param foraDoSla                 quantidade de chamados fora do SLA agora
 * @param tempoMedioResolucaoHoras  media de (resolvidoEm - criadoEm), 0 se nao houver nenhum
 * @param chamadosForaDoSla         do mais atrasado para o menos
 */
public record DashboardResumoResponse(
        Map<StatusChamado, Long> totalPorStatus,
        Map<Prioridade, Long> totalPorPrioridade,
        long totalChamados,
        long foraDoSla,
        double tempoMedioResolucaoHoras,
        List<ChamadoResumoResponse> chamadosForaDoSla) {
}
