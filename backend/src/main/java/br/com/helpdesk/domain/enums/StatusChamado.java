package br.com.helpdesk.domain.enums;

/**
 * Estados do chamado. As transicoes permitidas entre eles ficam em
 * {@link br.com.helpdesk.domain.state.TransicaoStatus} -- e em nenhum outro lugar.
 */
public enum StatusChamado {

    /** Criado, ainda sem atendente. */
    ABERTO,

    /** Um atendente assumiu (ou o chamado foi reaberto). */
    EM_ATENDIMENTO,

    /** O atendente concluiu; aguarda confirmacao do solicitante. */
    RESOLVIDO,

    /** O solicitante confirmou a resolucao. Estado final. */
    FECHADO;

    /** Status em que o chamado ainda consome SLA. */
    public boolean emAndamento() {
        return this == ABERTO || this == EM_ATENDIMENTO;
    }

    /** Status em que o chamado ja foi resolvido pelo atendente. */
    public boolean concluido() {
        return this == RESOLVIDO || this == FECHADO;
    }
}
