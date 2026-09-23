package br.com.helpdesk.web;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.exception.AcessoNegadoException;
import br.com.helpdesk.domain.exception.RecursoNaoEncontradoException;
import br.com.helpdesk.domain.exception.TransicaoInvalidaException;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.repository.UsuarioRepository;
import br.com.helpdesk.service.ChamadoService;
import br.com.helpdesk.support.UsuarioBuilder;
import br.com.helpdesk.web.controller.ChamadoController;
import br.com.helpdesk.web.dto.request.AlterarStatusRequest;
import br.com.helpdesk.web.dto.request.NovoChamadoRequest;
import br.com.helpdesk.web.filter.UsuarioAtualArgumentResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web: controller + Bean Validation + filtros + GlobalExceptionHandler.
 *
 * <p>Cobre o contrato de erro -- 400, 401, 403, 404 e 422 -- e o formato do JSON de erro.
 * A regra de negocio em si e testada nos unitarios; aqui so interessa a traducao para HTTP.</p>
 */
@WebMvcTest(ChamadoController.class)
@Import(UsuarioAtualArgumentResolver.class)
@DisplayName("ChamadoController (fatia web)")
class ChamadoControllerTest {

    private static final String HEADER_USUARIO = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChamadoService chamadoService;
    @MockBean
    private UsuarioRepository usuarioRepository;

    private final Usuario bruno = UsuarioBuilder.atendente().comId(2L).build();

    @BeforeEach
    void autenticarBruno() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(bruno));
    }

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    // ---------------------------------------------------------------- 401

    @Test
    @DisplayName("401 quando o header X-User-Id nao vem")
    void deveDevolver401SemHeaderDeUsuario() throws Exception {
        mockMvc.perform(get("/api/chamados"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void deveDevolver401QuandoOHeaderNaoENumerico() throws Exception {
        mockMvc.perform(get("/api/chamados").header(HEADER_USUARIO, "abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
    }

    @Test
    void deveDevolver401QuandoOUsuarioNaoExiste() throws Exception {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/chamados").header(HEADER_USUARIO, "404"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveDevolver401QuandoOUsuarioEstaInativo() throws Exception {
        when(usuarioRepository.findById(7L))
                .thenReturn(Optional.of(UsuarioBuilder.solicitante().comId(7L).inativo().build()));

        mockMvc.perform(get("/api/chamados").header(HEADER_USUARIO, "7"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("inativo")));
    }

    // ---------------------------------------------------------------- 400

    @Test
    @DisplayName("400 com a lista de campos invalidos quando o DTO nao passa na validacao")
    void deveDevolver400ComOsCamposInvalidos() throws Exception {
        NovoChamadoRequest invalido = new NovoChamadoRequest("", "curta", null, null);

        mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos[*].campo")
                        .value(org.hamcrest.Matchers.hasItems("titulo", "descricao", "categoriaId", "prioridade")));
    }

    @Test
    void deveDevolver400QuandoOCorpoEUmJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isso nao e json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO"));
    }

    @Test
    void deveDevolver400QuandoOFiltroDeStatusNaoEUmValorDoEnum() throws Exception {
        mockMvc.perform(get("/api/chamados").header(HEADER_USUARIO, "2").param("status", "INVENTADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO"));
    }

    // ---------------------------------------------------------------- 403 / 404 / 422

    @Test
    void deveMapearAcessoNegadoPara403() throws Exception {
        when(chamadoService.buscarDetalhe(eq(1L), any()))
                .thenThrow(new AcessoNegadoException("Voce so pode acessar os chamados que abriu"));

        mockMvc.perform(get("/api/chamados/1").header(HEADER_USUARIO, "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem").value("Voce so pode acessar os chamados que abriu"));
    }

    @Test
    void deveMapearRecursoNaoEncontradoPara404() throws Exception {
        when(chamadoService.buscarDetalhe(eq(99L), any()))
                .thenThrow(RecursoNaoEncontradoException.de("Chamado", 99L));

        mockMvc.perform(get("/api/chamados/99").header(HEADER_USUARIO, "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void deveMapearTransicaoInvalidaPara422() throws Exception {
        when(chamadoService.alterarStatus(anyLong(), any(), any()))
                .thenThrow(new TransicaoInvalidaException(StatusChamado.ABERTO, StatusChamado.RESOLVIDO,
                        "Chamado ABERTO nao pode ir direto para RESOLVIDO"));

        mockMvc.perform(patch("/api/chamados/1/status")
                        .header(HEADER_USUARIO, "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.RESOLVIDO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("TRANSICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Chamado ABERTO nao pode ir direto para RESOLVIDO"));
    }

    @Test
    void deveDevolver400QuandoOStatusDoCorpoEstaAusente() throws Exception {
        mockMvc.perform(patch("/api/chamados/1/status")
                        .header(HEADER_USUARIO, "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("status"));
    }

    // ---------------------------------------------------------------- X-Request-Id

    @Test
    @DisplayName("toda resposta traz X-Request-Id, gerado quando o cliente nao manda")
    void deveDevolverORequestIdGerado() throws Exception {
        mockMvc.perform(get("/api/chamados"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    @DisplayName("o X-Request-Id enviado pelo cliente e reaproveitado, nao substituido")
    void devePropagarORequestIdDoCliente() throws Exception {
        mockMvc.perform(get("/api/chamados")
                        .header("X-Request-Id", "id-vindo-do-frontend"))
                .andExpect(header().string("X-Request-Id", "id-vindo-do-frontend"));
    }

    // ---------------------------------------------------------------- caminho feliz

    @Test
    void deveDevolver201ComLocationAoAbrirChamado() throws Exception {
        when(chamadoService.abrir(any(), any())).thenReturn(ChamadoRespostas.chamadoAberto());

        mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoChamadoRequest("Notebook nao liga",
                                "A tela fica preta ao ligar.", 1L, Prioridade.ALTA))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/chamados/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }
}
