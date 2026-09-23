package br.com.helpdesk.web.controller;

import br.com.helpdesk.service.UsuarioService;
import br.com.helpdesk.web.dto.request.LoginRequest;
import br.com.helpdesk.web.dto.response.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Login simulado (ADR-003). As duas rotas sao publicas: o UsuarioAtualFilter deixa
 * /api/auth/** passar sem exigir X-User-Id -- seria um ciclo, ja que e aqui que o
 * usuario descobre o proprio id.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Login simulado do MVP. Nao ha senha nem token")
public class AuthController {

    private final UsuarioService usuarioService;

    @GetMapping("/usuarios-disponiveis")
    @Operation(summary = "Lista os usuarios que podem ser escolhidos na tela de login",
            description = "Publico. Use o id retornado aqui no header X-User-Id das demais chamadas")
    public List<UsuarioResponse> usuariosDisponiveis() {
        return usuarioService.listarDisponiveisParaLogin();
    }

    @PostMapping("/login")
    @Operation(summary = "Entra no sistema como o usuario informado",
            description = "Publico. Confirma que o usuario existe e esta ativo")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return usuarioService.autenticar(requisicao.usuarioId());
    }
}
