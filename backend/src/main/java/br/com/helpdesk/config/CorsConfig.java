package br.com.helpdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS liberado apenas para a SPA em desenvolvimento (ADR-014).
 *
 * <p>{@code X-Request-Id} precisa estar em {@code exposedHeaders}: sem isso o JavaScript
 * nao consegue ler o header e o q-notify de erro perde a correlacao com o log.</p>
 *
 * <p>Em producao a SPA e a API ficam sob a mesma origem (Nginx) e este bean deixa de ter
 * efeito.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String ORIGEM_SPA = "http://localhost:9000";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ORIGEM_SPA)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
