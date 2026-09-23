package br.com.helpdesk.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Primeiro filtro da cadeia (secao 4.11 do documento).
 *
 * <p>Gera ou reaproveita o {@code X-Request-Id}, coloca em {@code requestId} no MDC -- de
 * onde o pattern do Logback o le -- e devolve o mesmo id no header da resposta.</p>
 *
 * <p>Precisa vir antes do {@link UsuarioAtualFilter} para que ate uma requisicao rejeitada
 * com 401 apareca no log correlacionada.</p>
 */
@Component
@Order(RequestIdFilter.ORDEM)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final int ORDEM = 1;
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
