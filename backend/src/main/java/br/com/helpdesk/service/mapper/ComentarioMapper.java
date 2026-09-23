package br.com.helpdesk.service.mapper;

import br.com.helpdesk.domain.model.Comentario;
import br.com.helpdesk.web.dto.response.ComentarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComentarioMapper {

    private final UsuarioMapper usuarioMapper;

    public ComentarioResponse paraResponse(Comentario comentario) {
        return new ComentarioResponse(
                comentario.getId(),
                usuarioMapper.paraResumo(comentario.getAutor()),
                comentario.getTexto(),
                comentario.getCriadoEm());
    }
}
