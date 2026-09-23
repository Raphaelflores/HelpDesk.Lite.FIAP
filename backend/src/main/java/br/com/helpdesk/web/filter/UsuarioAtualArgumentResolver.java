package br.com.helpdesk.web.filter;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Entrega o {@link UsuarioAtual} que o {@link UsuarioAtualFilter} publicou no request para
 * qualquer metodo de controller que o declare como parametro.
 */
@Component
public class UsuarioAtualArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return UsuarioAtual.class.equals(parametro.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parametro,
                                  ModelAndViewContainer mav,
                                  NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        return request.getAttribute(UsuarioAtual.ATRIBUTO_REQUEST, RequestAttributes.SCOPE_REQUEST);
    }
}
