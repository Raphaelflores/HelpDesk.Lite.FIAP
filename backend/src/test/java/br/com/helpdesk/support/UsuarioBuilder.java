package br.com.helpdesk.support;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.model.Usuario;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Builder de usuario para testes.
 *
 * <p>Fixtures vem daqui, nunca do {@code data.sql}: um teste que depende do seed quebra
 * quando a demonstracao muda.</p>
 */
public final class UsuarioBuilder {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1000);

    private Long id;
    private String nome = "Usuario de Teste";
    private String email;
    private Perfil perfil = Perfil.SOLICITANTE;
    private boolean ativo = true;

    private UsuarioBuilder() {
    }

    public static UsuarioBuilder solicitante() {
        return new UsuarioBuilder().comPerfil(Perfil.SOLICITANTE).comNome("Ana Solicitante");
    }

    public static UsuarioBuilder atendente() {
        return new UsuarioBuilder().comPerfil(Perfil.ATENDENTE).comNome("Bruno Atendente");
    }

    public static UsuarioBuilder admin() {
        return new UsuarioBuilder().comPerfil(Perfil.ADMIN).comNome("Carla Admin");
    }

    public UsuarioBuilder comId(Long id) {
        this.id = id;
        return this;
    }

    public UsuarioBuilder comNome(String nome) {
        this.nome = nome;
        return this;
    }

    public UsuarioBuilder comEmail(String email) {
        this.email = email;
        return this;
    }

    public UsuarioBuilder comPerfil(Perfil perfil) {
        this.perfil = perfil;
        return this;
    }

    public UsuarioBuilder inativo() {
        this.ativo = false;
        return this;
    }

    /** Sem id explicito, gera um -- basta ser unico dentro do teste. */
    public Usuario build() {
        long identificador = id != null ? id : SEQUENCIA.incrementAndGet();
        return Usuario.builder()
                .id(id)
                .nome(nome)
                .email(email != null ? email : "usuario" + identificador + "@empresa.com")
                .perfil(perfil)
                .ativo(ativo)
                .build();
    }

    /** Para cenarios @DataJpaTest, em que o id vem do banco. */
    public Usuario buildSemId() {
        return comId(null).build();
    }
}
