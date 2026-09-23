package br.com.helpdesk.web.filter;

import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.repository.UsuarioRepository;
import br.com.helpdesk.web.exception.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Autenticacao simulada do MVP (ADR-003): le o header {@code X-User-Id}, carrega o usuario
 * e o publica no request como {@link UsuarioAtual}. Tambem coloca {@code usuarioId} no MDC.
 *
 * <p>Responde <b>401</b> se o header estiver ausente, nao for numerico, apontar para um
 * usuario inexistente ou para um usuario inativo.</p>
 *
 * <p>Este filtro <b>nao decide permissao</b> -- so identifica quem esta chamando.</p>
 *
 * <p>Como ele roda antes do DispatcherServlet, o {@code @RestControllerAdvice} nao alcanca
 * as excecoes daqui: o 401 e serializado na mao, no mesmo formato dos demais erros
 * (ADR-017).</p>
 */
@Component
@Order(UsuarioAtualFilter.ORDEM)
@RequiredArgsConstructor
@Slf4j
public class UsuarioAtualFilter extends OncePerRequestFilter {

    public static final int ORDEM = 2;
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String MDC_USUARIO_ID = "usuarioId";

    private static final String PREFIXO_PROTEGIDO = "/api/";
    private static final String PREFIXO_PUBLICO = "/api/auth/";

    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    /** So protege /api/**, menos /api/auth/**. Swagger, H2 e Actuator ficam de fora. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true; // preflight de CORS nao carrega header de usuario
        }
        String caminho = request.getRequestURI();
        return !caminho.startsWith(PREFIXO_PROTEGIDO) || caminho.startsWith(PREFIXO_PUBLICO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(header)) {
            responder401(response, "Header " + HEADER_USER_ID + " ausente");
            return;
        }

        Long usuarioId;
        try {
            usuarioId = Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            responder401(response, "Header " + HEADER_USER_ID + " deve ser um numero");
            return;
        }

        Optional<Usuario> encontrado = usuarioRepository.findById(usuarioId);
        if (encontrado.isEmpty()) {
            responder401(response, "Usuario " + usuarioId + " nao encontrado");
            return;
        }

        Usuario usuario = encontrado.get();
        if (!usuario.isAtivo()) {
            responder401(response, "Usuario " + usuarioId + " esta inativo");
            return;
        }

        request.setAttribute(UsuarioAtual.ATRIBUTO_REQUEST, new UsuarioAtual(usuario));
        MDC.put(MDC_USUARIO_ID, String.valueOf(usuario.getId()));
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USUARIO_ID);
        }
    }

    private void responder401(HttpServletResponse response, String mensagem) throws IOException {
        log.warn("Requisicao nao autenticada: {}", mensagem);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErroResponse.de(401, "NAO_AUTENTICADO", mensagem));
    }
}
