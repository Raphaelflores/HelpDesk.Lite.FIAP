package br.com.helpdesk.repository;

import br.com.helpdesk.domain.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    List<Comentario> findAllByChamadoIdOrderByCriadoEmAsc(Long chamadoId);
}
