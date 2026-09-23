package br.com.helpdesk.web;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.support.Fixtures;
import br.com.helpdesk.web.dto.response.CategoriaResponse;
import br.com.helpdesk.web.dto.response.ChamadoResponse;
import br.com.helpdesk.web.dto.response.UsuarioResumoResponse;

import java.util.List;

/** DTOs de resposta prontos para os testes de fatia web. */
final class ChamadoRespostas {

    private ChamadoRespostas() {
    }

    static ChamadoResponse chamadoAberto() {
        return new ChamadoResponse(
                1L,
                "Notebook nao liga",
                "A tela fica preta ao ligar.",
                StatusChamado.ABERTO,
                Prioridade.ALTA,
                new CategoriaResponse(1L, "TI - Infraestrutura", 4, true),
                new UsuarioResumoResponse(1L, "Ana Solicitante", Perfil.SOLICITANTE),
                null,
                Fixtures.AGORA,
                Fixtures.AGORA,
                null,
                null,
                Fixtures.horasAFrente(4),
                false,
                List.of());
    }
}
