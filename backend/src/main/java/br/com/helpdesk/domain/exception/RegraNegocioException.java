package br.com.helpdesk.domain.exception;

/**
 * Regra de dominio violada que nao e uma transicao de status invalida.
 * Vira HTTP 422 com codigo REGRA_NEGOCIO.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
