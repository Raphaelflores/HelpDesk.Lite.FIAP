package br.com.helpdesk.domain;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.TransicaoInvalidaException;
import br.com.helpdesk.domain.state.TransicaoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static br.com.helpdesk.domain.enums.Perfil.ADMIN;
import static br.com.helpdesk.domain.enums.Perfil.ATENDENTE;
import static br.com.helpdesk.domain.enums.Perfil.SOLICITANTE;
import static br.com.helpdesk.domain.enums.StatusChamado.ABERTO;
import static br.com.helpdesk.domain.enums.StatusChamado.EM_ATENDIMENTO;
import static br.com.helpdesk.domain.enums.StatusChamado.FECHADO;
import static br.com.helpdesk.domain.enums.StatusChamado.RESOLVIDO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Maquina de estados. Classe pura: nada de Spring, so {@code new}.
 *
 * <p>A cobertura aqui e exaustiva de proposito -- sao 4 status x 4 status x 3 perfis, e o
 * teste percorre as 48 combinacoes afirmando que exatamente 8 passam.</p>
 */
@DisplayName("TransicaoStatus")
class TransicaoStatusTest {

    private final TransicaoStatus transicaoStatus = new TransicaoStatus();

    @Nested
    @DisplayName("transicoes validas")
    class TransicoesValidas {

        @ParameterizedTest(name = "{0} -> {1} pelo perfil {2}")
        @CsvSource({
                "ABERTO,         EM_ATENDIMENTO, ATENDENTE",
                "ABERTO,         EM_ATENDIMENTO, ADMIN",
                "EM_ATENDIMENTO, RESOLVIDO,      ATENDENTE",
                "EM_ATENDIMENTO, RESOLVIDO,      ADMIN",
                "RESOLVIDO,      FECHADO,        SOLICITANTE",
                "RESOLVIDO,      FECHADO,        ADMIN",
                "RESOLVIDO,      EM_ATENDIMENTO, SOLICITANTE",
                "RESOLVIDO,      EM_ATENDIMENTO, ADMIN"
        })
        void deveAceitarAsTransicoesDaTabela(StatusChamado de, StatusChamado para, Perfil perfil) {
            assertThatCode(() -> transicaoStatus.validar(de, para, perfil)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("exatamente 8 combinacoes de status x perfil sao validas, e nenhuma outra")
        void deveRejeitarTodasAsCombinacoesForaDaTabela() {
            long validas = 0;

            for (StatusChamado de : StatusChamado.values()) {
                for (StatusChamado para : StatusChamado.values()) {
                    for (Perfil perfil : Perfil.values()) {
                        if (transicaoStatus.permitida(de, para, perfil)) {
                            validas++;
                        } else {
                            assertThatThrownBy(() -> transicaoStatus.validar(de, para, perfil))
                                    .as("%s -> %s por %s deveria ser invalida", de, para, perfil)
                                    .isInstanceOf(TransicaoInvalidaException.class);
                        }
                    }
                }
            }

            assertThat(validas).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("transicoes invalidas")
    class TransicoesInvalidas {

        @Test
        void deveRejeitarTransicaoDeAbertoParaResolvido() {
            assertThatThrownBy(() -> transicaoStatus.validar(ABERTO, RESOLVIDO, ATENDENTE))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessageContaining("ABERTO")
                    .hasMessageContaining("RESOLVIDO");
        }

        @Test
        void deveRejeitarTransicaoDeAbertoParaFechado() {
            assertThatThrownBy(() -> transicaoStatus.validar(ABERTO, FECHADO, ADMIN))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }

        @ParameterizedTest(name = "de FECHADO para {0}")
        @EnumSource(StatusChamado.class)
        void deveTratarFechadoComoEstadoFinal(StatusChamado destino) {
            for (Perfil perfil : Perfil.values()) {
                assertThat(transicaoStatus.permitida(FECHADO, destino, perfil))
                        .as("FECHADO -> %s por %s", destino, perfil)
                        .isFalse();
            }
        }

        @ParameterizedTest(name = "{0} -> {0}")
        @EnumSource(StatusChamado.class)
        void deveRejeitarTransicaoParaOMesmoStatus(StatusChamado status) {
            for (Perfil perfil : Perfil.values()) {
                assertThatThrownBy(() -> transicaoStatus.validar(status, status, perfil))
                        .isInstanceOf(TransicaoInvalidaException.class);
            }
        }

        @Test
        @DisplayName("o perfil errado na aresta certa tambem e transicao invalida")
        void deveRejeitarPerfilQueNaoExecutaAAresta() {
            assertThatThrownBy(() -> transicaoStatus.validar(ABERTO, EM_ATENDIMENTO, SOLICITANTE))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessageContaining("SOLICITANTE");
        }

        @Test
        void deveExporOsStatusDaTransicaoNaExcecao() {
            assertThatThrownBy(() -> transicaoStatus.validar(ABERTO, RESOLVIDO, ADMIN))
                    .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                        assertThat(e.getDe()).isEqualTo(ABERTO);
                        assertThat(e.getPara()).isEqualTo(RESOLVIDO);
                    });
        }
    }

    @Nested
    @DisplayName("reabertura")
    class Reabertura {

        @Test
        void deveReabrirApenasApartirDeResolvido() {
            assertThat(transicaoStatus.permitida(RESOLVIDO, EM_ATENDIMENTO, SOLICITANTE)).isTrue();
            assertThat(transicaoStatus.permitida(FECHADO, EM_ATENDIMENTO, SOLICITANTE)).isFalse();
            assertThat(transicaoStatus.permitida(FECHADO, EM_ATENDIMENTO, ADMIN)).isFalse();
        }

        @Test
        @DisplayName("reabertura e do solicitante e do admin, nao do atendente (ADR-016)")
        void deveRestringirReaberturaAoSolicitanteEAoAdmin() {
            assertThat(transicaoStatus.permitida(RESOLVIDO, EM_ATENDIMENTO, SOLICITANTE)).isTrue();
            assertThat(transicaoStatus.permitida(RESOLVIDO, EM_ATENDIMENTO, ADMIN)).isTrue();
            assertThat(transicaoStatus.permitida(RESOLVIDO, EM_ATENDIMENTO, ATENDENTE)).isFalse();
        }
    }

    @Nested
    @DisplayName("consultas auxiliares")
    class ConsultasAuxiliares {

        @Test
        void deveListarOsProximosStatusDeUmAtendente() {
            assertThat(transicaoStatus.proximosStatus(EM_ATENDIMENTO, ATENDENTE))
                    .containsExactly(RESOLVIDO);
            assertThat(transicaoStatus.proximosStatus(ABERTO, ATENDENTE))
                    .containsExactly(EM_ATENDIMENTO);
            assertThat(transicaoStatus.proximosStatus(RESOLVIDO, ATENDENTE)).isEmpty();
        }

        @Test
        void deveListarOsProximosStatusDeUmSolicitante() {
            assertThat(transicaoStatus.proximosStatus(RESOLVIDO, SOLICITANTE))
                    .containsExactlyInAnyOrder(FECHADO, EM_ATENDIMENTO);
            assertThat(transicaoStatus.proximosStatus(ABERTO, SOLICITANTE)).isEmpty();
        }

        @Test
        @DisplayName("ninguem consegue levar um chamado de volta para ABERTO")
        void deveDeixarAbertoInalcancavel() {
            assertThat(transicaoStatus.perfisQueLevamA(ABERTO)).isEmpty();
        }

        @Test
        void deveListarOsPerfisQueLevamACadaStatus() {
            assertThat(transicaoStatus.perfisQueLevamA(EM_ATENDIMENTO))
                    .isEqualTo(Set.of(ATENDENTE, ADMIN, SOLICITANTE));
            assertThat(transicaoStatus.perfisQueLevamA(RESOLVIDO))
                    .isEqualTo(Set.of(ATENDENTE, ADMIN));
            assertThat(transicaoStatus.perfisQueLevamA(FECHADO))
                    .isEqualTo(Set.of(SOLICITANTE, ADMIN));
        }
    }
}
