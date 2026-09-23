package br.com.helpdesk.web.exception;

import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.exception.TransicaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Comparator;
import java.util.List;

/**
 * Traducao unica de excecao para JSON (ADR-010).
 *
 * <p>Regra de log da secao 4.11 do documento: <b>4xx em WARN sem stack trace</b>,
 * <b>5xx em ERROR com stack trace</b>. Este e o unico lugar do sistema que loga excecao.</p>
 *
 * <p>O {@code requestId} nao entra na mensagem: ele ja vem do MDC no pattern do Logback.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** DTO reprovado na Bean Validation -> 400 com a lista de campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException e,
                                                        HttpServletRequest request) {
        List<ErroResponse.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroResponse.CampoInvalido::campo))
                .toList();

        logarClient(request, 400, "campos invalidos: " + campos.stream()
                .map(ErroResponse.CampoInvalido::campo).toList());

        return ResponseEntity.badRequest()
                .body(ErroResponse.deValidacao("Requisicao invalida", campos));
    }

    /** JSON malformado, enum inexistente no corpo, parametro de tipo errado -> 400. */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErroResponse> tratarRequisicaoMalformada(Exception e,
                                                                   HttpServletRequest request) {
        logarClient(request, 400, e.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(ErroResponse.de(400, "VALIDACAO", "Requisicao invalida ou malformada"));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AcessoNegadoException e,
                                                           HttpServletRequest request) {
        logarClient(request, 403, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResponse.de(403, "ACESSO_NEGADO", e.getMessage()));
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(Exception e,
                                                            HttpServletRequest request) {
        logarClient(request, 404, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(404, "RECURSO_NAO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarTransicaoInvalida(TransicaoInvalidaException e,
                                                                HttpServletRequest request) {
        logarClient(request, 422, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErroResponse.de(422, "TRANSICAO_INVALIDA", e.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraNegocio(RegraNegocioException e,
                                                           HttpServletRequest request) {
        logarClient(request, 422, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErroResponse.de(422, "REGRA_NEGOCIO", e.getMessage()));
    }

    /** Rede de seguranca: nada de stack trace no corpo da resposta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInterno(Exception e, HttpServletRequest request) {
        log.error("Erro interno em {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.de(500, "ERRO_INTERNO",
                        "Erro interno do servidor. Informe o X-Request-Id ao suporte."));
    }

    /** 4xx: WARN e sem stack trace -- erro de cliente nao e defeito do servidor. */
    private void logarClient(HttpServletRequest request, int status, Object detalhe) {
        log.warn("Requisicao rejeitada com {} em {} {}: {}",
                status, request.getMethod(), request.getRequestURI(), detalhe);
    }
}
