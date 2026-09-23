package br.com.helpdesk.web.controller;

import br.com.helpdesk.service.UsuarioService;
import br.com.helpdesk.web.dto.request.UsuarioRequest;
import br.com.helpdesk.web.dto.response.UsuarioResponse;
import br.com.helpdesk.web.filter.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Cadastro de usuarios e perfis (Admin)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Lista todos os usuarios, ativos e inativos (Admin)")
    public List<UsuarioResponse> listar(UsuarioAtual usuarioAtual) {
        return usuarioService.listar(usuarioAtual.usuario());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um usuario (Admin)")
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest requisicao,
                                 UsuarioAtual usuarioAtual) {
        return usuarioService.criar(requisicao, usuarioAtual.usuario());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuario (Admin)")
    public UsuarioResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody UsuarioRequest requisicao,
                                     UsuarioAtual usuarioAtual) {
        return usuarioService.atualizar(id, requisicao, usuarioAtual.usuario());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um usuario (Admin)",
            description = "Soft delete. Nao e possivel desativar o proprio usuario logado")
    public void desativar(@PathVariable Long id, UsuarioAtual usuarioAtual) {
        usuarioService.desativar(id, usuarioAtual.usuario());
    }
}
