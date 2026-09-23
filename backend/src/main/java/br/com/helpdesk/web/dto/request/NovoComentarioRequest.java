package br.com.helpdesk.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Comentario na linha do tempo. Imutavel apos a criacao")
public record NovoComentarioRequest(

        @Schema(example = "Ja reproduzi o erro aqui, vou investigar.")
        @NotBlank(message = "nao deve estar em branco")
        @Size(min = 2, max = 2000, message = "deve ter entre 2 e 2000 caracteres")
        String texto) {
}
