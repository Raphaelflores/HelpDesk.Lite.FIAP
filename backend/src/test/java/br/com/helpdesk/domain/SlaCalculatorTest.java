package br.com.helpdesk.domain;

import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.service.SlaCalculator;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.ChamadoBuilder;
import br.com.helpdesk.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Regras de SLA. Roda com {@code Clock.fixed}, entao cada cenario e deterministico -- sem
 * {@code Thread.sleep} e sem data "quase certa".
 */
@DisplayName("SlaCalculator")
class SlaCalculatorTest {

    private final SlaCalculator slaCalculator = new SlaCalculator(Fixtures.CLOCK_FIXO);

    @Nested
    @DisplayName("prazo")
    class Prazo {

        @Test
        void deveSomarOSlaDaCategoriaAoInstanteDeCriacao() {
            Chamado chamado = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(4).build())
                    .criadoEm(Fixtures.horasAtras(1))
                    .build();

            assertThat(slaCalculator.prazo(chamado)).isEqualTo(Fixtures.horasAFrente(3));
        }

        @Test
        void deveRespeitarSlasDiferentesPorCategoria() {
            Chamado curto = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(4).build())
                    .criadoEm(Fixtures.AGORA)
                    .build();
            Chamado longo = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(48).build())
                    .criadoEm(Fixtures.AGORA)
                    .build();

            assertThat(slaCalculator.prazo(curto)).isEqualTo(Fixtures.horasAFrente(4));
            assertThat(slaCalculator.prazo(longo)).isEqualTo(Fixtures.horasAFrente(48));
        }
    }

    @Nested
    @DisplayName("fora do SLA")
    class ForaDoSla {

        @Test
        void deveConsiderarForaDoSlaChamadoAbertoComPrazoVencido() {
            Chamado chamado = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(8).build())
                    .criadoEm(Fixtures.horasAtras(30))
                    .build();

            assertThat(slaCalculator.foraDoSla(chamado)).isTrue();
        }

        @Test
        void deveConsiderarDentroDoSlaChamadoAbertoComPrazoEmAberto() {
            Chamado chamado = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(8).build())
                    .criadoEm(Fixtures.horasAtras(2))
                    .build();

            assertThat(slaCalculator.foraDoSla(chamado)).isFalse();
        }

        @Test
        void deveConsiderarForaDoSlaChamadoEmAtendimentoComPrazoVencido() {
            Chamado chamado = ChamadoBuilder.emAtendimento()
                    .comCategoria(CategoriaBuilder.comSla(4).build())
                    .criadoEm(Fixtures.horasAtras(20))
                    .build();

            assertThat(slaCalculator.foraDoSla(chamado)).isTrue();
        }

        @Test
        void deveConsiderarForaDoSlaChamadoResolvidoDepoisDoPrazo() {
            Chamado chamado = ChamadoBuilder.resolvido()
                    .comCategoria(CategoriaBuilder.comSla(24).build())
                    .criadoEm(Fixtures.horasAtras(96))
                    .resolvidoEm(Fixtures.horasAtras(60))   // 36h de atendimento, prazo era 24h
                    .build();

            assertThat(slaCalculator.foraDoSla(chamado)).isTrue();
        }

        @Test
        @DisplayName("chamado antigo resolvido dentro do prazo continua dentro do SLA")
        void deveIgnorarAgoraQuandoOChamadoJaFoiResolvidoNoPrazo() {
            Chamado chamado = ChamadoBuilder.resolvido()
                    .comCategoria(CategoriaBuilder.comSla(24).build())
                    .criadoEm(Fixtures.horasAtras(500))
                    .resolvidoEm(Fixtures.horasAtras(490))  // 10h de atendimento, prazo era 24h
                    .build();

            assertThat(slaCalculator.foraDoSla(chamado)).isFalse();
        }

        @Test
        void deveConsiderarDentroDoSlaChamadoExatamenteNoPrazo() {
            Chamado chamado = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(4).build())
                    .criadoEm(Fixtures.horasAtras(4))       // vence exatamente agora
                    .build();

            assertThat(slaCalculator.foraDoSla(chamado)).isFalse();
        }

        @Test
        void deveAvaliarChamadoFechadoPeloInstanteDeResolucao() {
            Chamado dentro = ChamadoBuilder.fechado()
                    .comCategoria(CategoriaBuilder.comSla(4).build())
                    .criadoEm(Fixtures.horasAtras(48))
                    .resolvidoEm(Fixtures.horasAtras(46))
                    .build();
            Chamado fora = ChamadoBuilder.fechado()
                    .comCategoria(CategoriaBuilder.comSla(4).build())
                    .criadoEm(Fixtures.horasAtras(48))
                    .resolvidoEm(Fixtures.horasAtras(20))
                    .build();

            assertThat(slaCalculator.foraDoSla(dentro)).isFalse();
            assertThat(slaCalculator.foraDoSla(fora)).isTrue();
        }
    }

    @Nested
    @DisplayName("atraso")
    class Atraso {

        @Test
        void deveSerZeroQuandoDentroDoSla() {
            Chamado chamado = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(8).build())
                    .criadoEm(Fixtures.horasAtras(1))
                    .build();

            assertThat(slaCalculator.atraso(chamado)).isZero();
        }

        @Test
        void deveMedirOAtrasoDeChamadoAindaEmAberto() {
            Chamado chamado = ChamadoBuilder.aberto()
                    .comCategoria(CategoriaBuilder.comSla(8).build())
                    .criadoEm(Fixtures.horasAtras(30))      // venceu ha 22h
                    .build();

            assertThat(slaCalculator.atraso(chamado)).isEqualTo(Duration.ofHours(22));
        }

        @Test
        void deveMedirOAtrasoPeloInstanteDeResolucaoQuandoJaResolvido() {
            Chamado chamado = ChamadoBuilder.resolvido()
                    .comCategoria(CategoriaBuilder.comSla(24).build())
                    .criadoEm(Fixtures.horasAtras(96))
                    .resolvidoEm(Fixtures.horasAtras(60))   // 36h - 24h = 12h de atraso
                    .build();

            assertThat(slaCalculator.atraso(chamado)).isEqualTo(Duration.ofHours(12));
        }
    }

    @Nested
    @DisplayName("tempo medio de resolucao")
    class TempoMedio {

        @Test
        void deveSerZeroSemNenhumChamado() {
            assertThat(slaCalculator.tempoMedioResolucaoHoras(List.of())).isZero();
        }

        @Test
        void deveSerZeroQuandoNenhumChamadoFoiResolvido() {
            List<Chamado> chamados = List.of(
                    ChamadoBuilder.aberto().build(),
                    ChamadoBuilder.emAtendimento().build());

            assertThat(slaCalculator.tempoMedioResolucaoHoras(chamados)).isZero();
        }

        @Test
        void deveUsarOUnicoChamadoResolvidoQuandoSoHaUm() {
            Chamado chamado = ChamadoBuilder.resolvido()
                    .criadoEm(Fixtures.horasAtras(10))
                    .resolvidoEm(Fixtures.horasAtras(4))    // 6h
                    .build();

            assertThat(slaCalculator.tempoMedioResolucaoHoras(List.of(chamado)))
                    .isCloseTo(6d, within(0.001));
        }

        @Test
        void deveCalcularAMediaEntreVariosChamadosResolvidos() {
            List<Chamado> chamados = List.of(
                    ChamadoBuilder.resolvido()
                            .criadoEm(Fixtures.horasAtras(10)).resolvidoEm(Fixtures.horasAtras(8)).build(),   // 2h
                    ChamadoBuilder.resolvido()
                            .criadoEm(Fixtures.horasAtras(20)).resolvidoEm(Fixtures.horasAtras(16)).build(),  // 4h
                    ChamadoBuilder.fechado()
                            .criadoEm(Fixtures.horasAtras(30)).resolvidoEm(Fixtures.horasAtras(24)).build()); // 6h

            assertThat(slaCalculator.tempoMedioResolucaoHoras(chamados)).isCloseTo(4d, within(0.001));
        }

        @Test
        @DisplayName("chamados sem resolvidoEm nao entram na media")
        void deveIgnorarChamadosNaoResolvidos() {
            List<Chamado> chamados = List.of(
                    ChamadoBuilder.resolvido()
                            .criadoEm(Fixtures.horasAtras(10)).resolvidoEm(Fixtures.horasAtras(8)).build(),   // 2h
                    ChamadoBuilder.aberto().comStatus(StatusChamado.ABERTO).build(),
                    ChamadoBuilder.emAtendimento().build());

            assertThat(slaCalculator.tempoMedioResolucaoHoras(chamados)).isCloseTo(2d, within(0.001));
        }

        @Test
        void deveArredondarParaDuasCasas() {
            List<Chamado> chamados = List.of(
                    ChamadoBuilder.resolvido()
                            .criadoEm(Fixtures.horasAtras(10)).resolvidoEm(Fixtures.horasAtras(9)).build(),   // 1h
                    ChamadoBuilder.resolvido()
                            .criadoEm(Fixtures.horasAtras(10)).resolvidoEm(Fixtures.horasAtras(8)).build(),   // 2h
                    ChamadoBuilder.resolvido()
                            .criadoEm(Fixtures.horasAtras(10)).resolvidoEm(Fixtures.horasAtras(8)).build());  // 2h

            assertThat(slaCalculator.tempoMedioResolucaoHoras(chamados)).isEqualTo(1.67d);
        }
    }
}
