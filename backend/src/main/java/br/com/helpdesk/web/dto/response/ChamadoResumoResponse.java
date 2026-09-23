package br.com.helpdesk.web.dto.response;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;

import java.time.Instant;

/**
 * Chamado em listagem. Nao carrega descricao nem comentarios -- a tela de lista nao usa,
 * e trazer tudo multiplicaria o payload por pagina.
 */
public record ChamadoResumoResponse(
        Long id,
        String titulo,
        StatusChamado status,
        Prioridade prioridade,
        CategoriaResponse categoria,
        UsuarioResumoResponse solicitante,
        UsuarioResumoResponse atendente,
        Instant criadoEm,
        Instant prazoSla,
        boolean foraDoSla) {
}
