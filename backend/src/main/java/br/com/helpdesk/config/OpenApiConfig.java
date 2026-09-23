package br.com.helpdesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Metadados da especificacao OpenAPI exposta em /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI helpdeskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HelpDesk Lite API")
                        .version("0.0.1")
                        .description("""
                                API de chamados internos (TI, Facilities, RH).

                                **Autenticacao simulada.** Nao ha login real: envie o header
                                `X-User-Id` com o id do usuario em todas as chamadas sob `/api`,
                                exceto `/api/auth/**`. Use `GET /api/auth/usuarios-disponiveis`
                                para descobrir os ids do seed.

                                Trocar o `X-User-Id` e a forma de testar os tres perfis pelo Swagger.

                                Toda resposta traz o header `X-Request-Id`, que aparece nas linhas
                                de log daquela requisicao."""))
                .servers(List.of(new Server().url("http://localhost:8080").description("Ambiente local")));
    }
}
