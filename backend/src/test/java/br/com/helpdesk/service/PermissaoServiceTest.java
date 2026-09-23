package br.com.helpdesk.service;

import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.domain.state.TransicaoStatus;
import br.com.helpdesk.support.ChamadoBuilder;
import br.com.helpdesk.support.UsuarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cada linha da tabela de permissoes da secao 3 do documento, incluindo os casos
 * "so os seus".
 *
 * <p>Classe pura: instanciada com {@code new}, sem Spring e sem mock de contexto.</p>
 */
@DisplayName("PermissaoService")
class PermissaoServiceTest {

    private final PermissaoService permissaoService = new PermissaoService(new TransicaoStatus());

    private final Usuario ana = UsuarioBuilder.solicitante().comId(1L).build();
    private final Usuario outroSolicitante = UsuarioBuilder.solicitante().comId(9L).build();
    private final Usuario bruno = UsuarioBuilder.atendente().comId(2L).build();
    private final Usuario outroAtendente = UsuarioBuilder.atendente().comId(8L).build();
    private final Usuario carla = UsuarioBuilder.admin().comId(3L).build();

    /** Chamado da Ana, atendido pelo Bruno. */
    private Chamado chamadoDaAnaComBruno(StatusChamado status) {
        return ChamadoBuilder.aberto()
                .comStatus(status)
                .comSolicitante(ana)
                .comAtendente(bruno)
                .build();
    }

    @Nested
    @DisplayName("abrir chamado -- todos os perfis")
    class Abertura {

        @Test
        void deveLiberarAberturaParaTodosOsPerfis() {
            assertThatCode(() -> permissaoService.verificarAbertura(ana)).doesNotThrowAnyException();
            assertThatCode(() -> permissaoService.verificarAbertura(bruno)).doesNotThrowAnyException();
            assertThatCode(() -> permissaoService.verificarAbertura(carla)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("visualizar chamado")
    class Visualizacao {

        @Test
        void devePermitirQueOSolicitanteVejaOProprioChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.ABERTO);

            assertThatCode(() -> permissaoService.verificarVisualizacao(ana, chamado))
                    .doesNotThrowAnyException();
        }

        @Test
        void deveNegarQueOSolicitanteVejaChamadoDeOutro() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.ABERTO);

            assertThatThrownBy(() -> permissaoService.verificarVisualizacao(outroSolicitante, chamado))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void devePermitirQueAtendenteEAdminVejamQualquerChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.ABERTO);

            assertThatCode(() -> permissaoService.verificarVisualizacao(outroAtendente, chamado))
                    .doesNotThrowAnyException();
            assertThatCode(() -> permissaoService.verificarVisualizacao(carla, chamado))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("assumir chamado")
    class Assuncao {

        @Test
        void devePermitirQueAtendenteEAdminAssumam() {
            Chamado chamado = ChamadoBuilder.aberto().comSolicitante(ana).semAtendente().build();

            assertThatCode(() -> permissaoService.verificarAssuncao(bruno, chamado))
                    .doesNotThrowAnyException();
            assertThatCode(() -> permissaoService.verificarAssuncao(carla, chamado))
                    .doesNotThrowAnyException();
        }

        @Test
        void deveNegarQueOSolicitanteAssumaAteOProprioChamado() {
            Chamado chamado = ChamadoBuilder.aberto().comSolicitante(ana).semAtendente().build();

            assertThatThrownBy(() -> permissaoService.verificarAssuncao(ana, chamado))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }

    @Nested
    @DisplayName("alterar status -- atendente")
    class AlteracaoPeloAtendente {

        @Test
        void devePermitirQueOAtendenteResolvaOChamadoQueAtende() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatCode(() -> permissaoService
                    .verificarAlteracaoStatus(bruno, chamado, StatusChamado.RESOLVIDO))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("403 no chamado de outro atendente -- nao revela o status")
        void deveNegarQueOAtendenteResolvaChamadoDeOutro() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatThrownBy(() -> permissaoService
                    .verificarAlteracaoStatus(outroAtendente, chamado, StatusChamado.RESOLVIDO))
                    .isInstanceOf(AcessoNegadoException.class)
                    .hasMessageContaining("atende");
        }

        @Test
        void deveNegarQueOAtendenteFecheOChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.RESOLVIDO);

            assertThatThrownBy(() -> permissaoService
                    .verificarAlteracaoStatus(bruno, chamado, StatusChamado.FECHADO))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }

    @Nested
    @DisplayName("alterar status -- solicitante")
    class AlteracaoPeloSolicitante {

        @Test
        void devePermitirQueOSolicitanteFecheOProprioChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.RESOLVIDO);

            assertThatCode(() -> permissaoService
                    .verificarAlteracaoStatus(ana, chamado, StatusChamado.FECHADO))
                    .doesNotThrowAnyException();
        }

        @Test
        void devePermitirQueOSolicitanteReabraOProprioChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.RESOLVIDO);

            assertThatCode(() -> permissaoService
                    .verificarAlteracaoStatus(ana, chamado, StatusChamado.EM_ATENDIMENTO))
                    .doesNotThrowAnyException();
        }

        @Test
        void deveNegarQueOSolicitanteFecheChamadoDeOutro() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.RESOLVIDO);

            assertThatThrownBy(() -> permissaoService
                    .verificarAlteracaoStatus(outroSolicitante, chamado, StatusChamado.FECHADO))
                    .isInstanceOf(AcessoNegadoException.class)
                    .hasMessageContaining("abriu");
        }

        @Test
        void deveNegarQueOSolicitanteResolvaOProprioChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatThrownBy(() -> permissaoService
                    .verificarAlteracaoStatus(ana, chamado, StatusChamado.RESOLVIDO))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }

    @Nested
    @DisplayName("alterar status -- admin e alvos impossiveis")
    class AlteracaoPeloAdmin {

        @Test
        void devePermitirQueOAdminAltereParaQualquerAlvoAlcancavel() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatCode(() -> permissaoService
                    .verificarAlteracaoStatus(carla, chamado, StatusChamado.RESOLVIDO))
                    .doesNotThrowAnyException();
            assertThatCode(() -> permissaoService
                    .verificarAlteracaoStatus(carla, chamado, StatusChamado.FECHADO))
                    .doesNotThrowAnyException();
            assertThatCode(() -> permissaoService
                    .verificarAlteracaoStatus(carla, chamado, StatusChamado.EM_ATENDIMENTO))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ninguem, nem o admin, leva um chamado de volta para ABERTO")
        void deveNegarVoltarParaAberto() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatThrownBy(() -> permissaoService
                    .verificarAlteracaoStatus(carla, chamado, StatusChamado.ABERTO))
                    .isInstanceOf(AcessoNegadoException.class);
            assertThatThrownBy(() -> permissaoService
                    .verificarAlteracaoStatus(bruno, chamado, StatusChamado.ABERTO))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }

    @Nested
    @DisplayName("comentar")
    class Comentario {

        @Test
        void devePermitirQueOSolicitanteComenteNoProprioChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatCode(() -> permissaoService.verificarComentario(ana, chamado))
                    .doesNotThrowAnyException();
        }

        @Test
        void deveNegarQueOSolicitanteComenteEmChamadoDeOutro() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatThrownBy(() -> permissaoService.verificarComentario(outroSolicitante, chamado))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void devePermitirQueAtendenteEAdminComentemEmQualquerChamado() {
            Chamado chamado = chamadoDaAnaComBruno(StatusChamado.EM_ATENDIMENTO);

            assertThatCode(() -> permissaoService.verificarComentario(outroAtendente, chamado))
                    .doesNotThrowAnyException();
            assertThatCode(() -> permissaoService.verificarComentario(carla, chamado))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("cadastros e dashboard")
    class CadastrosEDashboard {

        @Test
        void deveRestringirGestaoDeCategoriasAoAdmin() {
            assertThatCode(() -> permissaoService.verificarGestaoCategorias(carla))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> permissaoService.verificarGestaoCategorias(bruno))
                    .isInstanceOf(AcessoNegadoException.class);
            assertThatThrownBy(() -> permissaoService.verificarGestaoCategorias(ana))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void deveRestringirGestaoDeUsuariosAoAdmin() {
            assertThatCode(() -> permissaoService.verificarGestaoUsuarios(carla))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> permissaoService.verificarGestaoUsuarios(bruno))
                    .isInstanceOf(AcessoNegadoException.class);
            assertThatThrownBy(() -> permissaoService.verificarGestaoUsuarios(ana))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        void deveLiberarODashboardParaAtendenteEAdminENegarParaSolicitante() {
            assertThatCode(() -> permissaoService.verificarDashboard(bruno)).doesNotThrowAnyException();
            assertThatCode(() -> permissaoService.verificarDashboard(carla)).doesNotThrowAnyException();
            assertThatThrownBy(() -> permissaoService.verificarDashboard(ana))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }
}
