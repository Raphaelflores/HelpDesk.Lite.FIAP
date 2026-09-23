package br.com.helpdesk.integration;

import br.com.helpdesk.domain.enums.Prioridade;
import br.com.helpdesk.domain.enums.StatusChamado;
import br.com.helpdesk.domain.model.Categoria;
import br.com.helpdesk.domain.model.Usuario;
import br.com.helpdesk.repository.CategoriaRepository;
import br.com.helpdesk.repository.UsuarioRepository;
import br.com.helpdesk.support.CategoriaBuilder;
import br.com.helpdesk.support.UsuarioBuilder;
import br.com.helpdesk.web.dto.request.AlterarStatusRequest;
import br.com.helpdesk.web.dto.request.NovoChamadoRequest;
import br.com.helpdesk.web.dto.request.NovoComentarioRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida completo do chamado pela API HTTP, trocando o {@code X-User-Id} a cada
 * passo: abrir (solicitante) -> assumir (atendente) -> comentar (os dois) -> resolver
 * (atendente) -> fechar (solicitante).
 *
 * <p>Perfil {@code test}: H2 limpo, sem {@code data.sql}. Os tres usuarios e a categoria
 * sao criados aqui, pelos builders de support.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Ciclo de vida do chamado (integracao)")
class CicloDeVidaChamadoIT {

    private static final String HEADER_USUARIO = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private Usuario ana;
    private Usuario bruno;
    private Usuario carla;
    private Categoria categoria;

    @BeforeEach
    void criarUsuariosECategoria() {
        ana = usuarioRepository.save(UsuarioBuilder.solicitante().comEmail("ana@it.com").buildSemId());
        bruno = usuarioRepository.save(UsuarioBuilder.atendente().comEmail("bruno@it.com").buildSemId());
        carla = usuarioRepository.save(UsuarioBuilder.admin().comEmail("carla@it.com").buildSemId());
        categoria = categoriaRepository.save(
                CategoriaBuilder.comSla(4).comNome("TI - Integracao").buildSemId());
    }

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    private JsonNode corpoDe(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("abrir -> assumir -> comentar -> resolver -> fechar, com tres usuarios diferentes")
    void devePercorrerOCicloDeVidaCompleto() throws Exception {
        // ---------- 1. Ana (solicitante) abre o chamado
        MvcResult aberto = mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoChamadoRequest("Notebook nao liga",
                                "A tela fica preta ao ligar, mesmo na tomada.",
                                categoria.getId(), Prioridade.ALTA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.atendente").doesNotExist())
                .andExpect(jsonPath("$.solicitante.id").value(ana.getId()))
                .andReturn();

        long chamadoId = corpoDe(aberto).get("id").asLong();

        // ---------- 2. Bruno (atendente) enxerga o chamado na fila e assume
        mockMvc.perform(get("/api/chamados")
                        .header(HEADER_USUARIO, bruno.getId())
                        .param("status", "ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/chamados/{id}/assumir", chamadoId)
                        .header(HEADER_USUARIO, bruno.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.atendente.id").value(bruno.getId()));

        // ---------- 3. Os dois comentam
        mockMvc.perform(post("/api/chamados/{id}/comentarios", chamadoId)
                        .header(HEADER_USUARIO, bruno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoComentarioRequest("Ja estou olhando, consegue mandar um print?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor.id").value(bruno.getId()));

        mockMvc.perform(post("/api/chamados/{id}/comentarios", chamadoId)
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoComentarioRequest("Enviei o print por e-mail."))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/chamados/{id}/comentarios", chamadoId)
                        .header(HEADER_USUARIO, ana.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // ---------- 4. Bruno resolve
        mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                        .header(HEADER_USUARIO, bruno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.RESOLVIDO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.resolvidoEm").isNotEmpty());

        // ---------- 5. Ana confirma e fecha
        MvcResult fechado = mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.FECHADO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"))
                .andExpect(jsonPath("$.fechadoEm").isNotEmpty())
                .andExpect(jsonPath("$.resolvidoEm").isNotEmpty())
                .andExpect(jsonPath("$.comentarios.length()").value(2))
                .andReturn();

        assertThat(corpoDe(fechado).get("atendente").get("id").asLong()).isEqualTo(bruno.getId());

        // ---------- 6. Carla (admin) ve o chamado no dashboard
        mockMvc.perform(get("/api/dashboard/resumo").header(HEADER_USUARIO, carla.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChamados").value(1))
                .andExpect(jsonPath("$.totalPorStatus.FECHADO").value(1));
    }

    @Test
    @DisplayName("o solicitante nao enxerga o chamado de outro solicitante em nenhum endpoint")
    void deveIsolarOsChamadosEntreSolicitantes() throws Exception {
        Usuario diego = usuarioRepository.save(
                UsuarioBuilder.solicitante().comNome("Diego").comEmail("diego@it.com").buildSemId());

        MvcResult aberto = mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoChamadoRequest("Chamado da Ana",
                                "Descricao do chamado da Ana.", categoria.getId(), Prioridade.BAIXA))))
                .andExpect(status().isCreated())
                .andReturn();

        long chamadoId = corpoDe(aberto).get("id").asLong();

        mockMvc.perform(get("/api/chamados").header(HEADER_USUARIO, diego.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/chamados/{id}", chamadoId).header(HEADER_USUARIO, diego.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));

        mockMvc.perform(get("/api/dashboard/resumo").header(HEADER_USUARIO, diego.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("reabrir volta o chamado para EM_ATENDIMENTO, zera resolvidoEm e mantem o atendente")
    void deveReabrirChamadoResolvido() throws Exception {
        MvcResult aberto = mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoChamadoRequest("Impressora sem toner",
                                "A impressora do terceiro andar parou.", categoria.getId(), Prioridade.MEDIA))))
                .andReturn();
        long chamadoId = corpoDe(aberto).get("id").asLong();

        mockMvc.perform(patch("/api/chamados/{id}/assumir", chamadoId)
                .header(HEADER_USUARIO, bruno.getId()));
        mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                        .header(HEADER_USUARIO, bruno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.RESOLVIDO))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.resolvidoEm").doesNotExist())
                .andExpect(jsonPath("$.atendente.id").value(bruno.getId()));
    }

    @Test
    @DisplayName("as duas rotas de auth sao publicas e nao exigem X-User-Id")
    void deveExporOLoginSimuladoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios-disponiveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\": " + carla.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    @DisplayName("as regras de permissao e de transicao valem de ponta a ponta")
    void deveAplicarPermissaoEDepoisTransicaoNaApi() throws Exception {
        MvcResult aberto = mockMvc.perform(post("/api/chamados")
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NovoChamadoRequest("Teclado quebrado",
                                "Algumas teclas pararam de funcionar.", categoria.getId(), Prioridade.BAIXA))))
                .andReturn();
        long chamadoId = corpoDe(aberto).get("id").asLong();

        // 403: a Ana nao pode resolver, mesmo sendo o chamado dela
        mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.RESOLVIDO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));

        // 422: a Ana pode fechar, mas ABERTO -> FECHADO nao existe
        mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                        .header(HEADER_USUARIO, ana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AlterarStatusRequest(StatusChamado.FECHADO))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("TRANSICAO_INVALIDA"));

        // 422: assumir duas vezes
        mockMvc.perform(patch("/api/chamados/{id}/assumir", chamadoId)
                        .header(HEADER_USUARIO, bruno.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/chamados/{id}/assumir", chamadoId)
                        .header(HEADER_USUARIO, bruno.getId()))
                .andExpect(status().isUnprocessableEntity());

        // 403: cadastros sao exclusivos do admin
        mockMvc.perform(get("/api/usuarios").header(HEADER_USUARIO, bruno.getId()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").header(HEADER_USUARIO, carla.getId()))
                .andExpect(status().isOk());
    }
}
