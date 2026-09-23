package br.com.helpdesk.domain.exception;

/**
 * Recurso inexistente. Vira HTTP 404 com codigo RECURSO_NAO_ENCONTRADO.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /** Fabrica para a mensagem padrao "Chamado 123 nao encontrado". */
    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException("%s %s nao encontrado".formatted(recurso, id));
    }
}
