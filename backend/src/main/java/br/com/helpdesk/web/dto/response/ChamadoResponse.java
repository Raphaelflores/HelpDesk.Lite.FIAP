package br.com.helpdesk.web.dto.response;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;

import java.time.Instant;
import java.util.List;

/** Detalhe do chamado, com a linha do tempo de comentarios. */
public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        StatusChamado status,
        Prioridade prioridade,
        CategoriaResponse categoria,
        UsuarioResumoResponse solicitante,
        UsuarioResumoResponse atendente,
        Instant criadoEm,
        Instant atualizadoEm,
        Instant resolvidoEm,
        Instant fechadoEm,
        Instant prazoSla,
        boolean foraDoSla,
        List<ComentarioResponse> comentarios) {
}
