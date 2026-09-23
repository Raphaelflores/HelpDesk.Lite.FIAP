package br.com.helpdesk.web.dto.request;

import br.com.helpdesk.domain.enums.StatusChamado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo status do chamado. A transicao e validada pela maquina de estados")
public record AlterarStatusRequest(

        @Schema(example = "RESOLVIDO", allowableValues = {"EM_ATENDIMENTO", "RESOLVIDO", "FECHADO"})
        @NotNull(message = "e obrigatorio")
        StatusChamado status) {
}
