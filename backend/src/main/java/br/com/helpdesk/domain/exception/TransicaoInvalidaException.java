package br.com.helpdesk.domain.exception;

import br.com.helpdesk.domain.enums.StatusChamado;

/**
 * A transicao pedida nao existe na maquina de estados, ou o perfil nao a executa.
 * Vira HTTP 422 com codigo TRANSICAO_INVALIDA.
 */
public class TransicaoInvalidaException extends RuntimeException {

    private final StatusChamado de;
    private final StatusChamado para;

    public TransicaoInvalidaException(StatusChamado de, StatusChamado para, String mensagem) {
        super(mensagem);
        this.de = de;
        this.para = para;
    }

    public StatusChamado getDe() {
        return de;
    }

    public StatusChamado getPara() {
        return para;
    }
}
