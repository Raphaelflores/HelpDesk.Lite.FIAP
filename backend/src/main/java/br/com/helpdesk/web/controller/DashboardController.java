package br.com.helpdesk.web.controller;

import br.com.helpdesk.service.DashboardService;
import br.com.helpdesk.web.dto.response.DashboardResumoResponse;
import br.com.helpdesk.web.filter.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores de atendimento e SLA")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumo")
    @Operation(summary = "Totais por status e prioridade, chamados fora do SLA e tempo medio",
            description = "Apenas atendente e admin. Cada leitura tambem atualiza os gauges "
                    + "helpdesk_chamados_por_status e helpdesk_chamados_fora_sla")
    public DashboardResumoResponse resumo(UsuarioAtual usuarioAtual) {
        return dashboardService.resumo(usuarioAtual.usuario());
    }
}
