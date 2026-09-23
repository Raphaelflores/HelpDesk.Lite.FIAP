package br.com.helpdesk.domain.state;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.TransicaoInvalidaException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.helpdesk.domain.enums.StatusChamado.ABERTO;
import static br.com.helpdesk.domain.enums.StatusChamado.EM_ATENDIMENTO;
import static br.com.helpdesk.domain.enums.StatusChamado.FECHADO;
import static br.com.helpdesk.domain.enums.StatusChamado.RESOLVIDO;

/**
 * Maquina de estados do chamado: a tabela {@code de -> para -> perfis} e o unico lugar do
 * sistema que sabe quais transicoes existem.
 *
 * <p>Classe pura de dominio: sem Spring, sem repositorio, sem HTTP. Instanciavel com
 * {@code new} num teste unitario.</p>
 *
 * <p>Divisao de responsabilidade com {@code PermissaoService} (ADR-015): aqui se responde
 * "essa transicao existe e esse perfil a executa?" (falha = 422). Quem responde
 * "esse usuario pode agir nesse chamado?" (falha = 403) e o PermissaoService, e ele roda
 * antes.</p>
 */
public class TransicaoStatus {

    /** Uma aresta da maquina de estados. */
    private record Transicao(StatusChamado de, StatusChamado para, Set<Perfil> perfis) {
    }

    private static final List<Transicao> TABELA = List.of(
            new Transicao(ABERTO, EM_ATENDIMENTO, EnumSet.of(Perfil.ATENDENTE, Perfil.ADMIN)),
            new Transicao(EM_ATENDIMENTO, RESOLVIDO, EnumSet.of(Perfil.ATENDENTE, Perfil.ADMIN)),
            new Transicao(RESOLVIDO, FECHADO, EnumSet.of(Perfil.SOLICITANTE, Perfil.ADMIN)),
            new Transicao(RESOLVIDO, EM_ATENDIMENTO, EnumSet.of(Perfil.SOLICITANTE, Perfil.ADMIN))
    );

    /**
     * Valida a transicao. Nao retorna nada: ou passa, ou lanca.
     *
     * @throws TransicaoInvalidaException se a aresta nao existe ou o perfil nao a executa
     */
    public void validar(StatusChamado de, StatusChamado para, Perfil perfil) {
        Transicao aresta = TABELA.stream()
                .filter(t -> t.de() == de && t.para() == para)
                .findFirst()
                .orElseThrow(() -> new TransicaoInvalidaException(de, para,
                        "Chamado %s nao pode ir direto para %s".formatted(de, para)));

        if (!aresta.perfis().contains(perfil)) {
            throw new TransicaoInvalidaException(de, para,
                    "O perfil %s nao executa a transicao de %s para %s".formatted(perfil, de, para));
        }
    }

    /** {@code true} se a aresta existe e o perfil a executa. Nao lanca. */
    public boolean permitida(StatusChamado de, StatusChamado para, Perfil perfil) {
        return TABELA.stream()
                .anyMatch(t -> t.de() == de && t.para() == para && t.perfis().contains(perfil));
    }

    /** Status alcancaveis a partir de {@code de} pelo perfil informado. */
    public Set<StatusChamado> proximosStatus(StatusChamado de, Perfil perfil) {
        return TABELA.stream()
                .filter(t -> t.de() == de && t.perfis().contains(perfil))
                .map(Transicao::para)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }

    /**
     * Perfis que conseguem levar um chamado ao status informado, por qualquer caminho.
     * E o que o PermissaoService usa para decidir 403 sem olhar o status atual (ADR-015).
     */
    public Set<Perfil> perfisQueLevamA(StatusChamado para) {
        Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);
        TABELA.stream().filter(t -> t.para() == para).forEach(t -> perfis.addAll(t.perfis()));
        return Collections.unmodifiableSet(perfis);
    }
}
