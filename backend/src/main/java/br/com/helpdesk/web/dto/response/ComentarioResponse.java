package br.com.helpdesk.web.dto.response;

import java.time.Instant;

public record ComentarioResponse(Long id, UsuarioResumoResponse autor, String texto, Instant criadoEm) {
}
