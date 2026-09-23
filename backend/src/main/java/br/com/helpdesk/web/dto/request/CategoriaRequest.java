package br.com.helpdesk.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro de categoria (apenas Admin)")
public record CategoriaRequest(

        @Schema(example = "TI - Infraestrutura")
        @NotBlank(message = "nao deve estar em branco")
        @Size(min = 2, max = 80, message = "deve ter entre 2 e 80 caracteres")
        String nome,

        @Schema(description = "Prazo de atendimento em horas", example = "4")
        @NotNull(message = "e obrigatorio")
        @Positive(message = "deve ser maior que zero")
        @Max(value = 8760, message = "deve ser no maximo 8760 (um ano)")
        Integer slaHoras) {
}
