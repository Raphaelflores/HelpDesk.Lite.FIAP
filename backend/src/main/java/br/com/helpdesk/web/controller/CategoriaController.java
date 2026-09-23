package br.com.helpdesk.web.controller;

import br.com.helpdesk.service.CategoriaService;
import br.com.helpdesk.web.dto.request.CategoriaRequest;
import br.com.helpdesk.web.dto.response.CategoriaResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Cadastro de categorias e seus SLAs")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "Lista categorias",
            description = "Por padrao so as ativas. Com incluirInativas=true (apenas Admin) "
                    + "devolve tambem as desativadas, para a tela de administracao")
    public List<CategoriaResponse> listar(
            UsuarioAtual usuarioAtual,
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativas) {
        return incluirInativas
                ? categoriaService.listarTodas(usuarioAtual.usuario())
                : categoriaService.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma categoria (Admin)")
    public CategoriaResponse criar(@Valid @RequestBody CategoriaRequest requisicao,
                                   UsuarioAtual usuarioAtual) {
        return categoriaService.criar(requisicao, usuarioAtual.usuario());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria (Admin)")
    public CategoriaResponse atualizar(@PathVariable Long id,
                                       @Valid @RequestBody CategoriaRequest requisicao,
                                       UsuarioAtual usuarioAtual) {
        return categoriaService.atualizar(id, requisicao, usuarioAtual.usuario());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa uma categoria (Admin)",
            description = "Soft delete: a categoria nunca e removida, para nao quebrar o "
                    + "historico dos chamados que a referenciam")
    public void desativar(@PathVariable Long id, UsuarioAtual usuarioAtual) {
        categoriaService.desativar(id, usuarioAtual.usuario());
    }
}
