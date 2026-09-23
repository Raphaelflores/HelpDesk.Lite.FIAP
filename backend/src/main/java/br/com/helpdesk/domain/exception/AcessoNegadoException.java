package br.com.helpdesk.domain.exception;

/**
 * O perfil do usuario, ou sua relacao com o recurso, nao permite a acao.
 * Vira HTTP 403 com codigo ACESSO_NEGADO.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
