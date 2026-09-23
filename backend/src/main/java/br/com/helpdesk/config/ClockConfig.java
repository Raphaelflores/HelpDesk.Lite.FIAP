package br.com.helpdesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relogio injetavel (ADR-013).
 *
 * <p>Nenhuma classe de dominio ou service chama {@code Instant.now()}: todas recebem este
 * bean. Nos testes ele e trocado por {@code Clock.fixed(...)}, o que torna SLA e carimbos
 * de tempo deterministicos.</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
