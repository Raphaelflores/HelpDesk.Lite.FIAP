package br.com.helpdesk.repository;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.ChamadoBuilder;
import br.com.helpdesk.support.Fixtures;
import br.com.helpdesk.support.UsuarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Filtros e visibilidade contra o H2 de verdade -- e a unica forma de provar que a regra
 * do solicitante esta no WHERE, e nao num filtro em memoria.
 *
 * <p>Perfil {@code test}: banco limpo, sem {@code data.sql}. As fixtures sao montadas aqui.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChamadoSpecification (@DataJpaTest)")
class ChamadoSpecificationTest {

    @Autowired
    private ChamadoRepository chamadoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private Usuario ana;
    private Usuario outroSolicitante;
    private Usuario bruno;
    private Usuario carla;
    private Categoria infra;
    private Categoria sistemas;

    private final Pageable primeiraPagina = PageRequest.of(0, 20);

    @BeforeEach
    void montarCenario() {
        ana = usuarioRepository.save(UsuarioBuilder.solicitante()
                .comEmail("ana@teste.com").buildSemId());
        outroSolicitante = usuarioRepository.save(UsuarioBuilder.solicitante()
                .comNome("Diego").comEmail("diego@teste.com").buildSemId());
        bruno = usuarioRepository.save(UsuarioBuilder.atendente()
                .comEmail("bruno@teste.com").buildSemId());
        carla = usuarioRepository.save(UsuarioBuilder.admin()
                .comEmail("carla@teste.com").buildSemId());

        infra = categoriaRepository.save(CategoriaBuilder.comSla(4).comNome("Infra").buildSemId());
        sistemas = categoriaRepository.save(CategoriaBuilder.comSla(8).comNome("Sistemas").buildSemId());

        // 3 chamados da Ana e 2 de outro solicitante.
        salvar(ana, null, infra, StatusChamado.ABERTO, Prioridade.ALTA);
        salvar(ana, bruno, sistemas, StatusChamado.EM_ATENDIMENTO, Prioridade.MEDIA);
        salvar(ana, bruno, infra, StatusChamado.RESOLVIDO, Prioridade.BAIXA);
        salvar(outroSolicitante, null, sistemas, StatusChamado.ABERTO, Prioridade.ALTA);
        salvar(outroSolicitante, carla, infra, StatusChamado.EM_ATENDIMENTO, Prioridade.ALTA);
    }

    private Chamado salvar(Usuario solicitante, Usuario atendente, Categoria categoria,
                           StatusChamado status, Prioridade prioridade) {
        Chamado chamado = ChamadoBuilder.aberto()
                .comStatus(status)
                .comPrioridade(prioridade)
                .comCategoria(categoria)
                .comSolicitante(solicitante)
                .comAtendente(atendente)
                .criadoEm(Fixtures.horasAtras(2))
                .buildSemId();
        if (status == StatusChamado.RESOLVIDO) {
            chamado.setResolvidoEm(Fixtures.horasAtras(1));
        }
        return chamadoRepository.save(chamado);
    }

    private Page<Chamado> buscar(Usuario usuario, StatusChamado status, Prioridade prioridade,
                                 Long categoriaId, boolean meus) {
        return chamadoRepository.findAll(ChamadoSpecification.comFiltros(
                usuario.getPerfil(), usuario.getId(), status, prioridade, categoriaId, meus),
                primeiraPagina);
    }

    @Test
    @DisplayName("o solicitante nao ve chamados de outros -- nem no conteudo, nem no count")
    void deveRestringirOSolicitanteAosPropriosChamados() {
        Page<Chamado> pagina = buscar(ana, null, null, null, false);

        assertThat(pagina.getContent()).hasSize(3);
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent())
                .allMatch(chamado -> chamado.getSolicitante().getId().equals(ana.getId()));
    }

    @Test
    @DisplayName("a visibilidade entra no count, entao a paginacao nao vaza registros alheios")
    void deveAplicarAVisibilidadeTambemNaContagemDePaginas() {
        Page<Chamado> primeira = chamadoRepository.findAll(
                ChamadoSpecification.comFiltros(Perfil.SOLICITANTE, ana.getId(),
                        null, null, null, false),
                PageRequest.of(0, 2));

        assertThat(primeira.getTotalElements()).isEqualTo(3);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.getContent()).hasSize(2);
    }

    @Test
    void deveDeixarAtendenteEAdminVeremABaseInteira() {
        assertThat(buscar(bruno, null, null, null, false).getTotalElements()).isEqualTo(5);
        assertThat(buscar(carla, null, null, null, false).getTotalElements()).isEqualTo(5);
    }

    @Test
    void deveFiltrarPorStatus() {
        assertThat(buscar(carla, StatusChamado.ABERTO, null, null, false).getTotalElements()).isEqualTo(2);
        assertThat(buscar(carla, StatusChamado.RESOLVIDO, null, null, false).getTotalElements()).isEqualTo(1);
        assertThat(buscar(carla, StatusChamado.FECHADO, null, null, false).getTotalElements()).isZero();
    }

    @Test
    void deveFiltrarPorPrioridade() {
        assertThat(buscar(carla, null, Prioridade.ALTA, null, false).getTotalElements()).isEqualTo(3);
        assertThat(buscar(carla, null, Prioridade.BAIXA, null, false).getTotalElements()).isEqualTo(1);
    }

    @Test
    void deveFiltrarPorCategoria() {
        assertThat(buscar(carla, null, null, infra.getId(), false).getTotalElements()).isEqualTo(3);
        assertThat(buscar(carla, null, null, sistemas.getId(), false).getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("meus=true e a fila pessoal do atendente")
    void deveFiltrarPelaFilaDoAtendente() {
        assertThat(buscar(bruno, null, null, null, true).getTotalElements()).isEqualTo(2);
        assertThat(buscar(carla, null, null, null, true).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("meus=true nao muda nada para o solicitante -- ele ja so ve os proprios")
    void deveIgnorarMeusParaOSolicitante() {
        assertThat(buscar(ana, null, null, null, true).getTotalElements()).isEqualTo(3);
    }

    @Test
    void deveCombinarVariosFiltrosDeUmaVez() {
        Page<Chamado> pagina = buscar(carla, StatusChamado.EM_ATENDIMENTO, Prioridade.ALTA,
                infra.getId(), false);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getSolicitante().getId())
                .isEqualTo(outroSolicitante.getId());
    }

    @Test
    @DisplayName("a visibilidade continua valendo mesmo com filtros que casariam com chamados alheios")
    void deveCombinarVisibilidadeComFiltros() {
        Page<Chamado> pagina = buscar(ana, StatusChamado.ABERTO, Prioridade.ALTA, null, false);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getSolicitante().getId()).isEqualTo(ana.getId());
    }

    @Test
    void deveDevolverPaginaVaziaQuandoNenhumChamadoCasa() {
        assertThat(buscar(ana, StatusChamado.FECHADO, null, null, false).getContent()).isEmpty();
    }
}
