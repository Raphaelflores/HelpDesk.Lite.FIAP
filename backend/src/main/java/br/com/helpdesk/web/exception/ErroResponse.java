package br.com.helpdesk.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Corpo unico de toda resposta de erro da API (secao 6 do documento).
 *
 * <p>{@code campos} so aparece no 400 de validacao -- nos outros casos e nulo e o Jackson
 * o omite.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        List<CampoInvalido> campos) {

    /** Um campo reprovado na Bean Validation. */
    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, null);
    }

    public static ErroResponse deValidacao(String mensagem, List<CampoInvalido> campos) {
        return new ErroResponse(Instant.now(), 400, "VALIDACAO", mensagem, campos);
    }
}
