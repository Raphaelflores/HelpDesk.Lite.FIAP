package br.com.helpdesk.web.dto.request;

import br.com.helpdesk.domain.enums.Prioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Abertura de chamado")
public record NovoChamadoRequest(

        @Schema(example = "Notebook nao liga")
        @NotBlank(message = "nao deve estar em branco")
        @Size(min = 5, max = 150, message = "deve ter entre 5 e 150 caracteres")
        String titulo,

        @Schema(example = "O notebook parou de ligar hoje de manha.")
        @NotBlank(message = "nao deve estar em branco")
        @Size(min = 10, max = 2000, message = "deve ter entre 10 e 2000 caracteres")
        String descricao,

        @Schema(example = "1")
        @NotNull(message = "e obrigatoria")
        Long categoriaId,

        @Schema(example = "ALTA")
        @NotNull(message = "e obrigatoria")
        Prioridade prioridade) {
}
