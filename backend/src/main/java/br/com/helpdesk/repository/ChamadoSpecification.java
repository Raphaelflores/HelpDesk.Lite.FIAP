package br.com.helpdesk.repository;

import br.com.helpdesk.domain.enums.Perfil;
import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Chamado;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinamicos da listagem de chamados (ADR-007).
 *
 * <p>Alem dos quatro filtros opcionais, esta classe injeta a <b>regra de visibilidade</b>:
 * quando o perfil e SOLICITANTE, um predicado {@code solicitante_id = :usuarioAtual} entra
 * no WHERE. Como ele esta na query e nao num filtro em memoria, nem a paginacao nem o
 * {@code count} vazam a existencia de chamados alheios.</p>
 */
public final class ChamadoSpecification {

    private ChamadoSpecification() {
    }

    /**
     * @param perfil        perfil do usuario logado -- define a visibilidade
     * @param usuarioAtualId id do usuario logado
     * @param status        filtro opcional
     * @param prioridade    filtro opcional
     * @param categoriaId   filtro opcional
     * @param meus          para atendente/admin, restringe a fila pessoal (atendente_id)
     */
    public static Specification<Chamado> comFiltros(Perfil perfil,
                                                    Long usuarioAtualId,
                                                    StatusChamado status,
                                                    Prioridade prioridade,
                                                    Long categoriaId,
                                                    boolean meus) {
        return (raiz, consulta, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            // Visibilidade: o solicitante so enxerga os proprios chamados, sempre.
            if (perfil == Perfil.SOLICITANTE) {
                predicados.add(cb.equal(raiz.get("solicitante").get("id"), usuarioAtualId));
            } else if (meus) {
                // Para atendente e admin, "meus" e a fila que a pessoa atende.
                predicados.add(cb.equal(raiz.get("atendente").get("id"), usuarioAtualId));
            }

            if (status != null) {
                predicados.add(cb.equal(raiz.get("status"), status));
            }
            if (prioridade != null) {
                predicados.add(cb.equal(raiz.get("prioridade"), prioridade));
            }
            if (categoriaId != null) {
                predicados.add(cb.equal(raiz.get("categoria").get("id"), categoriaId));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
