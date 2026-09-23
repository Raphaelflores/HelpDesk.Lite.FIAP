package br.com.helpdesk.web.dto.response;

import br.com.helpdesk.domain.enums.Perfil;

/** Usuario embutido dentro de outro recurso (solicitante, atendente, autor). */
public record UsuarioResumoResponse(Long id, String nome, Perfil perfil) {
}
