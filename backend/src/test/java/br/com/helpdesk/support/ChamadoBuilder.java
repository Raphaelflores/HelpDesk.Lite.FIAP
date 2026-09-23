package br.com.helpdesk.support;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Usuario;

import java.time.Instant;

/**
 * Builder de chamado para testes.
 *
 * <p>Os atalhos {@code aberto()}, {@code emAtendimento()}, {@code resolvido()} e
 * {@code fechado()} ja deixam o chamado num estado coerente (atendente e carimbos de tempo
 * compativeis com o status), para o teste so precisar declarar o que e relevante para ele.</p>
 */
public final class ChamadoBuilder {

    private Long id = 1L;
    private String titulo = "Chamado de teste";
    private String descricao = "Descricao suficientemente longa para passar na validacao.";
    private StatusChamado status = StatusChamado.ABERTO;
    private Prioridade prioridade = Prioridade.MEDIA;
    private Categoria categoria = CategoriaBuilder.comSla(8).comId(1L).build();
    private Usuario solicitante = UsuarioBuilder.solicitante().comId(1L).build();
    private Usuario atendente;
    private Instant criadoEm = Fixtures.horasAtras(1);
    private Instant atualizadoEm = Fixtures.horasAtras(1);
    private Instant resolvidoEm;
    private Instant fechadoEm;

    private ChamadoBuilder() {
    }

    public static ChamadoBuilder aberto() {
        return new ChamadoBuilder().comStatus(StatusChamado.ABERTO);
    }

    public static ChamadoBuilder emAtendimento() {
        return new ChamadoBuilder()
                .comStatus(StatusChamado.EM_ATENDIMENTO)
                .comAtendente(UsuarioBuilder.atendente().comId(2L).build());
    }

    public static ChamadoBuilder resolvido() {
        return new ChamadoBuilder()
                .comStatus(StatusChamado.RESOLVIDO)
                .comAtendente(UsuarioBuilder.atendente().comId(2L).build())
                .criadoEm(Fixtures.horasAtras(5))
                .resolvidoEm(Fixtures.horasAtras(1));
    }

    public static ChamadoBuilder fechado() {
        return resolvido()
                .comStatus(StatusChamado.FECHADO)
                .fechadoEm(Fixtures.AGORA);
    }

    public ChamadoBuilder comId(Long id) {
        this.id = id;
        return this;
    }

    public ChamadoBuilder comTitulo(String titulo) {
        this.titulo = titulo;
        return this;
    }

    public ChamadoBuilder comStatus(StatusChamado status) {
        this.status = status;
        return this;
    }

    public ChamadoBuilder comPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
        return this;
    }

    public ChamadoBuilder comCategoria(Categoria categoria) {
        this.categoria = categoria;
        return this;
    }

    public ChamadoBuilder comSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
        return this;
    }

    public ChamadoBuilder comAtendente(Usuario atendente) {
        this.atendente = atendente;
        return this;
    }

    public ChamadoBuilder semAtendente() {
        this.atendente = null;
        return this;
    }

    public ChamadoBuilder criadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
        this.atualizadoEm = criadoEm;
        return this;
    }

    public ChamadoBuilder resolvidoEm(Instant resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
        return this;
    }

    public ChamadoBuilder fechadoEm(Instant fechadoEm) {
        this.fechadoEm = fechadoEm;
        return this;
    }

    public Chamado build() {
        return Chamado.builder()
                .id(id)
                .titulo(titulo)
                .descricao(descricao)
                .status(status)
                .prioridade(prioridade)
                .categoria(categoria)
                .solicitante(solicitante)
                .atendente(atendente)
                .criadoEm(criadoEm)
                .atualizadoEm(atualizadoEm)
                .resolvidoEm(resolvidoEm)
                .fechadoEm(fechadoEm)
                .build();
    }

    public Chamado buildSemId() {
        return comId(null).build();
    }
}
