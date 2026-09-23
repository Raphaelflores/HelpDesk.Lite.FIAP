package br.com.helpdesk.support;

import br.com.helpdesk.domain.model.Categoria;

import java.util.concurrent.atomic.AtomicLong;

public final class CategoriaBuilder {

    private static final AtomicLong SEQUENCIA = new AtomicLong(2000);

    private Long id;
    private String nome;
    private Integer slaHoras = 8;
    private boolean ativa = true;

    private CategoriaBuilder() {
    }

    public static CategoriaBuilder categoria() {
        return new CategoriaBuilder();
    }

    /** Atalho para o caso mais comum nos testes: so o SLA importa. */
    public static CategoriaBuilder comSla(int horas) {
        return new CategoriaBuilder().comSlaHoras(horas);
    }

    public CategoriaBuilder comId(Long id) {
        this.id = id;
        return this;
    }

    public CategoriaBuilder comNome(String nome) {
        this.nome = nome;
        return this;
    }

    public CategoriaBuilder comSlaHoras(Integer slaHoras) {
        this.slaHoras = slaHoras;
        return this;
    }

    public CategoriaBuilder inativa() {
        this.ativa = false;
        return this;
    }

    public Categoria build() {
        long identificador = id != null ? id : SEQUENCIA.incrementAndGet();
        return Categoria.builder()
                .id(id)
                .nome(nome != null ? nome : "Categoria " + identificador)
                .slaHoras(slaHoras)
                .ativa(ativa)
                .build();
    }

    public Categoria buildSemId() {
        return comId(null).build();
    }
}
