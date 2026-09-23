package br.com.helpdesk.service;

import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import br.com.helpdesk.repository.CategoriaRepository;
import br.com.helpdesk.repository.ChamadoRepository;
import br.com.helpdesk.service.mapper.CategoriaMapper;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.UsuarioBuilder;
import br.com.helpdesk.web.dto.request.CategoriaRequest;
import br.com.helpdesk.web.dto.response.CategoriaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaService")
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ChamadoRepository chamadoRepository;

    private CategoriaService categoriaService;

    private final Usuario ana = UsuarioBuilder.solicitante().comId(1L).build();
    private final Usuario bruno = UsuarioBuilder.atendente().comId(2L).build();
    private final Usuario carla = UsuarioBuilder.admin().comId(3L).build();

    @BeforeEach
    void prepararServico() {
        categoriaService = new CategoriaService(categoriaRepository, chamadoRepository,
                new PermissaoService(new TransicaoStatus()), new CategoriaMapper());
    }

    @Test
    void deveListarApenasAsCategoriasAtivasParaOFormularioDeChamado() {
        when(categoriaRepository.findAllByAtivaTrueOrderByNomeAsc())
                .thenReturn(List.of(CategoriaBuilder.comSla(4).comId(1L).comNome("TI").build()));

        List<CategoriaResponse> categorias = categoriaService.listarAtivas();

        assertThat(categorias).singleElement()
                .satisfies(c -> {
                    assertThat(c.nome()).isEqualTo("TI");
                    assertThat(c.slaHoras()).isEqualTo(4);
                    assertThat(c.ativa()).isTrue();
                });
    }

    @Test
    void deveIncluirAsInativasNaListagemAdministrativa() {
        when(categoriaRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(
                CategoriaBuilder.comSla(4).comId(1L).build(),
                CategoriaBuilder.comSla(8).comId(2L).inativa().build()));

        assertThat(categoriaService.listarTodas(carla)).hasSize(2);
    }

    @Test
    void deveNegarListagemAdministrativaParaQuemNaoEAdmin() {
        assertThatThrownBy(() -> categoriaService.listarTodas(bruno))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void deveCriarCategoriaAtivaAparandoONome() {
        when(categoriaRepository.existsByNomeIgnoreCase("Facilities")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        categoriaService.criar(new CategoriaRequest("  Facilities  ", 24), carla);

        ArgumentCaptor<Categoria> capturada = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(capturada.capture());
        assertThat(capturada.getValue().getNome()).isEqualTo("Facilities");
        assertThat(capturada.getValue().getSlaHoras()).isEqualTo(24);
        assertThat(capturada.getValue().isAtiva()).isTrue();
    }

    @Test
    void deveRejeitarNomeDuplicadoNaCriacao() {
        when(categoriaRepository.existsByNomeIgnoreCase("Facilities")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.criar(new CategoriaRequest("Facilities", 24), carla))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Ja existe");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void deveNegarCriacaoParaSolicitanteEAtendente() {
        CategoriaRequest requisicao = new CategoriaRequest("Facilities", 24);

        assertThatThrownBy(() -> categoriaService.criar(requisicao, ana))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> categoriaService.criar(requisicao, bruno))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void deveAtualizarNomeESla() {
        Categoria existente = CategoriaBuilder.comSla(8).comId(5L).comNome("RH").build();
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNomeIgnoreCaseAndIdNot("RH e DP", 5L)).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        CategoriaResponse resposta =
                categoriaService.atualizar(5L, new CategoriaRequest("RH e DP", 48), carla);

        assertThat(resposta.nome()).isEqualTo("RH e DP");
        assertThat(resposta.slaHoras()).isEqualTo(48);
    }

    @Test
    void deveRejeitarNomeDuplicadoDeOutraCategoriaNaAtualizacao() {
        when(categoriaRepository.findById(5L))
                .thenReturn(Optional.of(CategoriaBuilder.comSla(8).comId(5L).build()));
        when(categoriaRepository.existsByNomeIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.atualizar(5L, new CategoriaRequest("TI", 4), carla))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void deveDevolver404AoAtualizarCategoriaInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.atualizar(99L, new CategoriaRequest("TI", 4), carla))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("excluir e sempre soft delete -- categoria com chamados nunca e removida")
    void deveDesativarEmVezDeExcluir() {
        Categoria existente = CategoriaBuilder.comSla(8).comId(5L).build();
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(chamadoRepository.existsByCategoriaId(5L)).thenReturn(true);

        categoriaService.desativar(5L, carla);

        assertThat(existente.isAtiva()).isFalse();
        verify(categoriaRepository).save(existente);
        verify(categoriaRepository, never()).delete(any());
        verify(categoriaRepository, never()).deleteById(anyLong());
    }

    @Test
    void deveNegarDesativacaoParaQuemNaoEAdmin() {
        assertThatThrownBy(() -> categoriaService.desativar(5L, bruno))
                .isInstanceOf(AcessoNegadoException.class);
        verify(categoriaRepository, never()).save(any());
    }
}
