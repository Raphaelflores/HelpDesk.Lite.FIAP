package br.com.helpdesk.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Pontos fixos compartilhados pelos testes.
 *
 * <p>O relogio e sempre {@link #CLOCK_FIXO}: nenhum teste depende de "agora" de verdade,
 * nem de {@code Thread.sleep}. Cenarios de SLA sao escritos em relacao a {@link #AGORA}.</p>
 */
public final class Fixtures {

    /** 21/09/2026, 12:00 UTC. Data arbitraria, so precisa ser estavel. */
    public static final Instant AGORA = Instant.parse("2026-09-21T12:00:00Z");

    public static final Clock CLOCK_FIXO = Clock.fixed(AGORA, ZoneOffset.UTC);

    private Fixtures() {
    }

    /** Instante a N horas atras de {@link #AGORA}. */
    public static Instant horasAtras(long horas) {
        return AGORA.minusSeconds(horas * 3600);
    }

    /** Instante a N horas a frente de {@link #AGORA}. */
    public static Instant horasAFrente(long horas) {
        return AGORA.plusSeconds(horas * 3600);
    }
}
