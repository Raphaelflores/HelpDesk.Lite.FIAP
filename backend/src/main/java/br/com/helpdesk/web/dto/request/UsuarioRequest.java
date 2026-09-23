package br.com.helpdesk.web.dto.request;

import br.com.helpdesk.domain.enums.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro de usuario (apenas Admin)")
public record UsuarioRequest(

        @Schema(example = "Ana Solicitante")
        @NotBlank(message = "nao deve estar em branco")
        @Size(min = 3, max = 120, message = "deve ter entre 3 e 120 caracteres")
        String nome,

        @Schema(example = "ana@empresa.com")
        @NotBlank(message = "nao deve estar em branco")
        @Email(message = "deve ser um e-mail valido")
        @Size(max = 150, message = "deve ter no maximo 150 caracteres")
        String email,

        @Schema(example = "SOLICITANTE")
        @NotNull(message = "e obrigatorio")
        Perfil perfil) {
}
