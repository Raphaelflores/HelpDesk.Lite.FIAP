package br.com.helpdesk.web.dto.response;

import br.com.helpdesk.domain.enums.Perfil;

/** Usuario devolvido pelo login simulado e pelo CRUD de usuarios. */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo) {
}
