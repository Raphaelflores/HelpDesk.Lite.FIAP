package br.com.helpdesk.web.filter;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.model.Usuario;

/**
 * O usuario autenticado da requisicao atual (ADR-003).
 *
 * <p>Publicado pelo {@link UsuarioAtualFilter} como atributo do request e injetado nos
 * controllers pelo {@link UsuarioAtualArgumentResolver}. Nunca vive em {@code ThreadLocal}
 * nem em contexto estatico (ADR-013): quem precisa dele recebe por parametro.</p>
 *
 * <p>Envolve a entidade de dominio ja carregada, para que os services recebam um
 * {@code Usuario} e a camada de servico nao precise conhecer nenhum tipo de {@code web}.</p>
 */
public record UsuarioAtual(Usuario usuario) {

    public static final String ATRIBUTO_REQUEST = "usuarioAtual";

    public Long id() {
        return usuario.getId();
    }

    public Perfil perfil() {
        return usuario.getPerfil();
    }

    public String nome() {
        return usuario.getNome();
    }
}
