package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.RegraNegocioException;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import br.com.helpdesk.repository.UsuarioRepository;
import br.com.helpdesk.service.mapper.UsuarioMapper;
import br.com.helpdesk.support.UsuarioBuilder;
import br.com.helpdesk.web.dto.request.UsuarioRequest;
import br.com.helpdesk.web.dto.response.UsuarioResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    private final Usuario ana = UsuarioBuilder.solicitante().comId(1L).build();
    private final Usuario bruno = UsuarioBuilder.atendente().comId(2L).build();
    private final Usuario carla = UsuarioBuilder.admin().comId(3L).build();

    @BeforeEach
    void prepararServico() {
        usuarioService = new UsuarioService(usuarioRepository,
                new PermissaoService(new TransicaoStatus()), new UsuarioMapper());
    }

    @Test
    void deveListarSomenteUsuariosAtivosNaTelaDeLogin() {
        when(usuarioRepository.findAllByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(ana, bruno, carla));

        List<UsuarioResponse> disponiveis = usuarioService.listarDisponiveisParaLogin();

        assertThat(disponiveis).hasSize(3)
                .extracting(UsuarioResponse::perfil)
                .containsExactly(Perfil.SOLICITANTE, Perfil.ATENDENTE, Perfil.ADMIN);
    }

    @Test
    void deveAutenticarUsuarioAtivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));

        assertThat(usuarioService.autenticar(1L).id()).isEqualTo(1L);
    }

    @Test
    void deveRecusarLoginDeUsuarioInativo() {
        Usuario inativo = UsuarioBuilder.solicitante().comId(7L).inativo().build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> usuarioService.autenticar(7L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveRecusarLoginDeUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.autenticar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveRestringirAListagemAdministrativaAoAdmin() {
        when(usuarioRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(ana, bruno, carla));

        assertThat(usuarioService.listar(carla)).hasSize(3);
        assertThatThrownBy(() -> usuarioService.listar(bruno)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> usuarioService.listar(ana)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("o e-mail e normalizado para minusculas na criacao")
    void deveCriarUsuarioNormalizandoOEmail() {
        when(usuarioRepository.existsByEmailIgnoreCase("Novo@Empresa.com".toLowerCase())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.criar(new UsuarioRequest("  Novo Usuario  ", "  Novo@Empresa.com  ",
                Perfil.ATENDENTE), carla);

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertThat(capturado.getValue().getEmail()).isEqualTo("novo@empresa.com");
        assertThat(capturado.getValue().getNome()).isEqualTo("Novo Usuario");
        assertThat(capturado.getValue().isAtivo()).isTrue();
    }

    @Test
    void deveRejeitarEmailDuplicadoNaCriacao() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(
                new UsuarioRequest("Ana", "ana@empresa.com", Perfil.SOLICITANTE), carla))
                .isInstanceOf(RegraNegocioException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveAtualizarNomeEmailEPerfil() {
        Usuario existente = UsuarioBuilder.solicitante().comId(5L).build();
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("novo@empresa.com", 5L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resposta = usuarioService.atualizar(5L,
                new UsuarioRequest("Novo Nome", "novo@empresa.com", Perfil.ATENDENTE), carla);

        assertThat(resposta.nome()).isEqualTo("Novo Nome");
        assertThat(resposta.email()).isEqualTo("novo@empresa.com");
        assertThat(resposta.perfil()).isEqualTo(Perfil.ATENDENTE);
    }

    @Test
    void deveRejeitarEmailJaUsadoPorOutroUsuario() {
        when(usuarioRepository.findById(5L))
                .thenReturn(Optional.of(UsuarioBuilder.solicitante().comId(5L).build()));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("ana@empresa.com", 5L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizar(5L,
                new UsuarioRequest("Ana", "ana@empresa.com", Perfil.SOLICITANTE), carla))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void deveDesativarEmVezDeExcluir() {
        Usuario alvo = UsuarioBuilder.solicitante().comId(5L).build();
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(alvo));

        usuarioService.desativar(5L, carla);

        assertThat(alvo.isAtivo()).isFalse();
        verify(usuarioRepository).save(alvo);
        verify(usuarioRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("o admin nao pode desativar a si mesmo -- ficaria sem conseguir entrar de volta")
    void deveImpedirQueOAdminDesativeOProprioUsuario() {
        assertThatThrownBy(() -> usuarioService.desativar(3L, carla))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("proprio");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveDevolver404AoDesativarUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.desativar(99L, carla))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveNegarGestaoDeUsuariosParaQuemNaoEAdmin() {
        UsuarioRequest requisicao = new UsuarioRequest("X", "x@empresa.com", Perfil.SOLICITANTE);

        assertThatThrownBy(() -> usuarioService.criar(requisicao, bruno))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> usuarioService.atualizar(1L, requisicao, ana))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> usuarioService.desativar(1L, bruno))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
