package br.com.helpdesk.web.controller;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.service.ChamadoService;
import br.com.helpdesk.web.dto.request.AlterarStatusRequest;
import br.com.helpdesk.web.dto.request.NovoChamadoRequest;
import br.com.helpdesk.web.dto.request.NovoComentarioRequest;
import br.com.helpdesk.web.dto.response.ChamadoResponse;
import br.com.helpdesk.web.dto.response.ChamadoResumoResponse;
import br.com.helpdesk.web.dto.response.ComentarioResponse;
import br.com.helpdesk.web.filter.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas de chamado. O controller so traduz HTTP: nao tem nenhuma regra de negocio,
 * nenhum {@code if} de permissao e nenhum acesso a repositorio.
 */
@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
@Tag(name = "Chamados", description = "Abertura, triagem e acompanhamento de chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    @GetMapping
    @Operation(summary = "Lista chamados paginados",
            description = "Filtros opcionais e combinaveis. O solicitante so recebe os "
                    + "proprios chamados -- a restricao entra no WHERE da consulta, entao "
                    + "vale tambem para o totalElements.")
    public Page<ChamadoResumoResponse> listar(
            UsuarioAtual usuarioAtual,
            @Parameter(description = "Filtra por status") @RequestParam(required = false) StatusChamado status,
            @Parameter(description = "Filtra por prioridade") @RequestParam(required = false) Prioridade prioridade,
            @Parameter(description = "Filtra por categoria") @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Para atendente e admin, restringe a fila que a pessoa atende")
            @RequestParam(required = false, defaultValue = "false") boolean meus,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable paginacao) {

        return chamadoService.listar(usuarioAtual.usuario(), status, prioridade, categoriaId,
                meus, paginacao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do chamado, com a linha do tempo de comentarios")
    public ChamadoResponse buscar(@PathVariable Long id, UsuarioAtual usuarioAtual) {
        return chamadoService.buscarDetalhe(id, usuarioAtual.usuario());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre um chamado", description = "Disponivel para todos os perfis")
    public ResponseEntity<ChamadoResponse> abrir(@Valid @RequestBody NovoChamadoRequest requisicao,
                                                 UsuarioAtual usuarioAtual) {
        ChamadoResponse criado = chamadoService.abrir(requisicao, usuarioAtual.usuario());
        return ResponseEntity.created(java.net.URI.create("/api/chamados/" + criado.id())).body(criado);
    }

    @PatchMapping("/{id}/assumir")
    @Operation(summary = "Atendente assume o chamado",
            description = "Define o atendente e leva o chamado para EM_ATENDIMENTO. "
                    + "Um chamado ja assumido devolve 422.")
    public ChamadoResponse assumir(@PathVariable Long id, UsuarioAtual usuarioAtual) {
        return chamadoService.assumir(id, usuarioAtual.usuario());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status do chamado",
            description = "403 quando o perfil ou a relacao com o chamado nao permitem a acao; "
                    + "422 quando a transicao nao existe a partir do status atual.")
    public ChamadoResponse alterarStatus(@PathVariable Long id,
                                         @Valid @RequestBody AlterarStatusRequest requisicao,
                                         UsuarioAtual usuarioAtual) {
        return chamadoService.alterarStatus(id, requisicao.status(), usuarioAtual.usuario());
    }

    @GetMapping("/{id}/comentarios")
    @Operation(summary = "Lista os comentarios do chamado")
    public List<ComentarioResponse> listarComentarios(@PathVariable Long id, UsuarioAtual usuarioAtual) {
        return chamadoService.listarComentarios(id, usuarioAtual.usuario());
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona um comentario", description = "Comentarios sao imutaveis")
    public ComentarioResponse comentar(@PathVariable Long id,
                                       @Valid @RequestBody NovoComentarioRequest requisicao,
                                       UsuarioAtual usuarioAtual) {
        return chamadoService.comentar(id, requisicao, usuarioAtual.usuario());
    }
}
