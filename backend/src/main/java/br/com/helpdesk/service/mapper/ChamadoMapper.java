package br.com.helpdesk.service.mapper;

import br.com.helpdesk.domain.model.Chamado;
import br.com.helpdesk.domain.model.Comentario;
import br.com.helpdesk.service.SlaCalculator;
import br.com.helpdesk.web.dto.response.ChamadoResponse;
import br.com.helpdesk.web.dto.response.ChamadoResumoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entidade -> DTO do chamado.
 *
 * <p>O prazo e o "fora do SLA" sao calculados aqui, no momento da resposta: nao sao
 * colunas do banco, sao derivados de criadoEm + slaHoras da categoria.</p>
 */
@Component
@RequiredArgsConstructor
public class ChamadoMapper {

    private final CategoriaMapper categoriaMapper;
    private final UsuarioMapper usuarioMapper;
    private final ComentarioMapper comentarioMapper;
    private final SlaCalculator slaCalculator;

    public ChamadoResumoResponse paraResumo(Chamado chamado) {
        return new ChamadoResumoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getStatus(),
                chamado.getPrioridade(),
                categoriaMapper.paraResponse(chamado.getCategoria()),
                usuarioMapper.paraResumo(chamado.getSolicitante()),
                usuarioMapper.paraResumo(chamado.getAtendente()),
                chamado.getCriadoEm(),
                slaCalculator.prazo(chamado),
                slaCalculator.foraDoSla(chamado));
    }

    public ChamadoResponse paraResponse(Chamado chamado, List<Comentario> comentarios) {
        return new ChamadoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus(),
                chamado.getPrioridade(),
                categoriaMapper.paraResponse(chamado.getCategoria()),
                usuarioMapper.paraResumo(chamado.getSolicitante()),
                usuarioMapper.paraResumo(chamado.getAtendente()),
                chamado.getCriadoEm(),
                chamado.getAtualizadoEm(),
                chamado.getResolvidoEm(),
                chamado.getFechadoEm(),
                slaCalculator.prazo(chamado),
                slaCalculator.foraDoSla(chamado),
                comentarios.stream().map(comentarioMapper::paraResponse).toList());
    }
}
