package br.com.helpdesk.service.mapper;

import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.web.dto.response.UsuarioResponse;
import br.com.helpdesk.web.dto.response.UsuarioResumoResponse;
import org.springframework.stereotype.Component;

/** Mapeamento manual entidade -> DTO (ADR-006). */
@Component
public class UsuarioMapper {

    public UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo());
    }

    /** Versao enxuta usada dentro de chamado e comentario. Nao expoe e-mail. */
    public UsuarioResumoResponse paraResumo(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoResponse(usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }
}
