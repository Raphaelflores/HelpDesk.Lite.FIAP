package br.com.helpdesk.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Login simulado: identifica o usuario apenas pelo id (ADR-003)")
public record LoginRequest(

        @Schema(description = "Id de um usuario ativo", example = "1")
        @NotNull(message = "e obrigatorio")
        Long usuarioId) {
}
