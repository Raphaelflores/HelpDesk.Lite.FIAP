package br.com.helpdesk.config;

import br.com.helpdesk.domain.state.TransicaoStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expoe as classes puras de dominio como beans.
 *
 * <p>{@link TransicaoStatus} nao leva {@code @Component} de proposito: o dominio nao
 * conhece Spring. Quem faz a ponte e esta configuracao, que vive na camada de fora.</p>
 */
@Configuration
public class DominioConfig {

    @Bean
    public TransicaoStatus transicaoStatus() {
        return new TransicaoStatus();
    }
}
