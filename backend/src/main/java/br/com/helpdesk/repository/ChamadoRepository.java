package br.com.helpdesk.repository;

import br.com.helpdesk.domain.model.Chamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChamadoRepository extends JpaRepository<Chamado, Long>,
        JpaSpecificationExecutor<Chamado> {

    /** Detalhe: traz categoria, solicitante e atendente numa consulta so. */
    @EntityGraph(attributePaths = {"categoria", "solicitante", "atendente"})
    Optional<Chamado> findWithRelacionamentosById(Long id);

    /** Base do dashboard: a agregacao de SLA precisa da categoria de cada chamado. */
    @EntityGraph(attributePaths = {"categoria", "solicitante", "atendente"})
    @Query("select c from Chamado c")
    List<Chamado> buscarTodosParaDashboard();

    boolean existsByCategoriaId(Long categoriaId);
}
