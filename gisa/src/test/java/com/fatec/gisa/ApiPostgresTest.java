package com.fatec.gisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fatec.gisa.validation.CpfValidator;
import com.jayway.jsonpath.JsonPath;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

// API completa contra um PostgreSQL real (embarcado, sem Docker), com todas as migrations do Flyway.
// - ddl-auto=validate: o Hibernate confere se as entidades batem com o esquema das migrations
// - chamadas HTTP com o administrador criado pela migration V6
// - open-in-view desligado: qualquer acesso tardio a relacionamento fora da transação quebraria aqui
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class ApiPostgresTest {

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    // Uma página com 6 terapias, cada uma com paciente, responsável, especialista, cargo e especialidade
    // próprios. Sem carregamento em lote, cada item dispararia consultas próprias (N+1).
    @Test
    void listagemDeTerapiasNaoFazUmaConsultaPorItem() throws Exception {
        for (int i = 0; i < 6; i++) {
            criarCenario();
        }
        var estatisticas = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        estatisticas.clear();

        chamar(get("/api/terapias?size=6")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6));

        long consultas = estatisticas.getPrepareStatementCount();
        System.out.println("Consultas SQL para listar 6 terapias: " + consultas);
        assertThat(consultas).isLessThanOrEqualTo(15);
    }

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    private static final String CPF_ADMIN = "99999999999";
    // E-mail de login do administrador semeado (migration V16)
    private static final String EMAIL_ADMIN = "admin@gisa.local";

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void encerrar() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void entrarComoAdministrador() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"" + EMAIL_ADMIN + "\", \"senha\": \"Admin@12345\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(resposta, "$.token");
    }

    @Test
    void todasAsMigracoesForamAplicadasComOsIndices() {
        Integer ultimaVersao = jdbcTemplate.queryForObject(
                "select max(cast(version as integer)) from flyway_schema_history where success", Integer.class);
        List<String> indices = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where indexname like 'idx_%'", String.class);

        assertThat(ultimaVersao).isEqualTo(20);
        assertThat(indices).contains(
                "idx_pessoa_nome_trgm", "idx_pessoa_cpf_trgm", "idx_escola_nome_trgm", "idx_cbo_titulo_trgm",
                "idx_terapia_status_data_inicio", "idx_especialista_terapia_especialista",
                "idx_paciente_terapia_terapia", "idx_log_auditoria_data_hora", "idx_evento_paciente_data_hora");
    }

    // V15: lista definitiva de permissões e permissões exatas dos perfis padrão. V18: permissão
    // VISUALIZAR_PACIENTES_VINCULADOS, que substitui VISUALIZAR_PACIENTES no perfil "Assistente Social"
    @Test
    void migracaoCriaAsPermissoesEOsPerfisPadrao() {
        assertThat(jdbcTemplate.queryForObject(
                "select \"Nome\" from \"Permissao\" where \"IDPermissao\" = 5", String.class))
                .isEqualTo("GERENCIAR_USUARIOS");
        assertThat(jdbcTemplate.queryForList("select \"Nome\" from \"Permissao\"", String.class))
                .hasSize(27)
                .doesNotContain("CADASTRAR_USUARIO")
                .contains("VISUALIZAR_PACIENTES", "VISUALIZAR_PACIENTES_VINCULADOS", "EMITIR_RECUSA",
                        "GERENCIAR_ESPECIALIDADES", "VISUALIZAR_AUDITORIA");

        assertThat(permissoesDoPerfil("Administrador")).containsExactly("ACESSO_TOTAL");
        assertThat(permissoesDoPerfil("Recepção")).containsExactly(
                "AGENDAR_TERAPIA", "ALTERAR_STATUS_TERAPIA", "EDITAR_PACIENTE", "EDITAR_TERAPIA", "EMITIR_RECUSA",
                "REGISTRAR_PRESENCA", "VISUALIZAR_HISTORICO_PACIENTE", "VISUALIZAR_PACIENTES", "VISUALIZAR_TERAPIAS");
        assertThat(permissoesDoPerfil("Especialista")).containsExactly(
                "EMITIR_ATESTADO", "EMITIR_RECEITA", "VISUALIZAR_HISTORICO_PACIENTE", "VISUALIZAR_PACIENTES");
        assertThat(permissoesDoPerfil("Assistente Social")).containsExactly(
                "VISUALIZAR_HISTORICO_PACIENTE", "VISUALIZAR_PACIENTES_VINCULADOS");
    }

    private List<String> permissoesDoPerfil(String perfil) {
        return jdbcTemplate.queryForList("""
                select permissao."Nome" from "Perfil" perfil
                join "Perfil_Permissao" vinculo on vinculo."IDPerfil" = perfil."IDPerfil"
                join "Permissao" permissao on permissao."IDPermissao" = vinculo."IDPermissao"
                where perfil."Nome" = ? order by permissao."Nome"
                """, String.class, perfil);
    }

    // Cria um cenário completo pela API e consulta todas as listagens e detalhes
    @Test
    void listagensEDetalhesFuncionamSemOpenInView() throws Exception {
        Cenario cenario = criarCenario();

        for (String rota : List.of(
                "/api/pacientes?termo=paciente", "/api/pacientes?termo=" + cenario.cpfPaciente(),
                "/api/pacientes/" + cenario.idPaciente(),
                "/api/responsaveis", "/api/responsaveis/" + cenario.idResponsavel(),
                "/api/profissionais", "/api/profissionais/" + cenario.idEspecialista(),
                "/api/especialistas/" + cenario.idEspecialista(), "/api/especialistas/pj/" + cenario.idPJ(),
                "/api/especialidades", "/api/cargos", "/api/cargos/1", "/api/cbos?titulo=teste", "/api/cbos/999999",
                "/api/perfis", "/api/perfis/1", "/api/permissoes",
                "/api/terapias", "/api/terapias?status=AGENDADA", "/api/terapias/" + cenario.idTerapia(),
                "/api/escolas", "/api/escolas?nome=escola&tipoEscola=PUBLICA", "/api/escolas/sugestoes?nome=escola",
                "/api/especialidades/1", "/api/usuarios", "/api/usuarios?tipoCadastro=PESSOA&idPerfil=1",
                "/api/pacientes?idEscola=1", "/api/cids/F84.0", "/api/auditoria")) {
            assertThat(chamar(get(rota)).andReturn().getResponse().getStatus())
                    .as("GET %s", rota)
                    .isEqualTo(200);
        }

        chamar(get("/api/pacientes/" + cenario.idPaciente()))
                .andExpect(jsonPath("$.responsaveis[0].responsavel.nome").exists())
                .andExpect(jsonPath("$.cids[0].codigoCID").value("F84.0"));
        chamar(get("/api/terapias/" + cenario.idTerapia()))
                .andExpect(jsonPath("$.especialistas[0].especialidades[0]").value("Psicologia"));
    }

    @Test
    void escolaSemNomeRetornaSugestoes() throws Exception {
        criarEscola();

        chamar(get("/api/escolas/sugestoes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].idEscola").exists());
        chamar(get("/api/escolas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantidadePacientes").exists())
                .andExpect(jsonPath("$.page.size").value(9));
    }

    @Test
    void errosSeguemOFormatoPadrao() throws Exception {
        chamar(get("/api/profissionais/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Profissional não encontrado com ID: 987654"))
                .andExpect(jsonPath("$.path").value("/api/profissionais/987654"));
        chamar(get("/api/especialistas/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Especialista não encontrado com ID: 987654"));
        chamar(get("/api/especialistas/pj/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Especialista PJ não encontrado com ID: 987654"));
        chamar(get("/api/cids/XYZ.9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("CID não encontrado: XYZ.9"));
        chamar(get("/api/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rota não encontrada."));
        chamar(get("/api/terapias?status=XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'status': XYZ"));
        chamar(get("/api/profissionais/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'idCadastro': abc"));
        chamar(delete("/api/cbos"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("O método DELETE não é suportado nesta rota."));
    }

    @Test
    void conflitoDeHorarioETravaFuncionamNoPostgres() throws Exception {
        Cenario cenario = criarCenario();
        Long outroPaciente = criarPaciente();

        chamar(post("/api/terapias").contentType(MediaType.APPLICATION_JSON).content("""
                { "pacientesIds": [%d], "especialistasIds": [%d],
                  "data": "%s", "horaInicio": "09:30", "horaTermino": "10:30" }
                """.formatted(outroPaciente, cenario.idEspecialista(), cenario.dataTerapia())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.startsWith("Conflito de horário: Especialista")));
    }

    @Test
    void faltaJustificadaLiberaOHorarioNoPostgres() throws Exception {
        Cenario cenario = criarCenario();

        chamar(patch("/api/terapias/" + cenario.idTerapia() + "/pacientes/" + cenario.idPaciente() + "/presenca")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"presenca\": \"FALTA_JUSTIFICADA\", \"justificativa\": \"Atestado médico\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarioLiberado").value(true))
                .andExpect(jsonPath("$.pacientes[0].justificativa").value("Atestado médico"));

        // Mesmo especialista e horário: aceito, porque o horário foi liberado
        chamar(post("/api/terapias").contentType(MediaType.APPLICATION_JSON).content("""
                { "pacientesIds": [%d], "especialistasIds": [%d],
                  "data": "%s", "horaInicio": "09:00", "horaTermino": "10:00" }
                """.formatted(criarPaciente(), cenario.idEspecialista(), cenario.dataTerapia())))
                .andExpect(status().isCreated());

        chamar(patch("/api/terapias/" + cenario.idTerapia() + "/pacientes/" + cenario.idPaciente() + "/presenca")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"presenca\": \"PENDENTE\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.startsWith("Não é possível desfazer a falta justificada")));
    }

    @Test
    void auditoriaRegistraLoginAlteracoesEConsultasSensiveis() throws Exception {
        Long idPaciente = criarPaciente();
        chamar(get("/api/pacientes/" + idPaciente)).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"" + EMAIL_ADMIN + "\", \"senha\": \"errada\" }"))
                .andExpect(status().isUnauthorized());
        // Listagens comuns não são auditadas
        chamar(get("/api/pacientes?termo=Paciente")).andExpect(status().isOk());

        List<String> registros = jdbcTemplate.queryForList("""
                select "Acao" || ' ' || "Metodo" || ' ' || "Caminho" || ' ' || "StatusHttp" || ' ' || coalesce("CPFUsuario", '-')
                from "LogAuditoria" order by "IDLog"
                """, String.class);

        // Só as tentativas de login que falham são registradas
        assertThat(registros).contains(
                "CRIACAO POST /api/pacientes 201 " + CPF_ADMIN,
                "CONSULTA GET /api/pacientes/" + idPaciente + " 200 " + CPF_ADMIN,
                "LOGIN POST /api/auth/login 401 " + CPF_ADMIN);
        assertThat(registros).doesNotContain(
                "CONSULTA GET /api/pacientes 200 " + CPF_ADMIN,
                "LOGIN POST /api/auth/login 200 " + CPF_ADMIN);

        chamar(get("/api/auditoria?acao=LOGIN&cpf=" + CPF_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].acao").value("LOGIN"))
                .andExpect(jsonPath("$.content[0].nomeUsuario").value("Administrador do Sistema"));
    }

    @Test
    void filtrosDaListagemDeTerapiasFuncionamNoPostgres() throws Exception {
        Cenario cenario = criarCenario();
        criarCenario();

        chamar(get("/api/terapias?data=%s&idPaciente=%d&idEspecialista=%d&status=AGENDADA&size=100&sort=dataHoraInicio,asc"
                .formatted(cenario.dataTerapia(), cenario.idPaciente(), cenario.idEspecialista())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].idTerapia").value(cenario.idTerapia()))
                .andExpect(jsonPath("$.page.size").value(100));
        chamar(get("/api/terapias?inicio=%s&fim=%s&idPaciente=%d"
                .formatted(cenario.dataTerapia(), cenario.dataTerapia(), cenario.idPaciente())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        chamar(get("/api/terapias?data=2026-13-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'data': 2026-13-01"));
        chamar(get("/api/terapias?data=2026-10-01&inicio=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Use 'data' ou o intervalo 'inicio'/'fim', não os dois juntos."));
        chamar(get("/api/terapias?inicio=2026-10-10&fim=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A data final deve ser igual ou posterior à data inicial."));
        chamar(get("/api/terapias?idPaciente=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'idPaciente': abc"));
        chamar(get("/api/terapias?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O parâmetro 'size' deve ser no máximo 100."));
        chamar(get("/api/terapias/minhas?data=2026-10-01&fim=2026-10-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    @Test
    void cancelamentoDeTerapiaPelaApi() throws Exception {
        Cenario cenario = criarCenario();
        String rota = "/api/terapias/" + cenario.idTerapia() + "/status";

        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"CANCELADA\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe o motivo do cancelamento."));
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"XYZ\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido ou malformado."));
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.status").value("Informe o status"));
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"status\": \"CANCELADA\", \"motivo\": \"%s\" }".formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.motivo").value("O motivo deve ter no máximo 255 caracteres"));

        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"status\": \"CANCELADA\", \"motivo\": \"Especialista em licença\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTerapia").value(cenario.idTerapia()))
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Especialista em licença"));
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"AGENDADA\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A terapia está cancelada e não pode mais mudar de status."));
        chamar(patch("/api/terapias/987654/status").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"status\": \"EM_ATENDIMENTO\" }"))
                .andExpect(status().isNotFound());

        // O horário cancelado fica livre para o mesmo especialista
        chamar(post("/api/terapias").contentType(MediaType.APPLICATION_JSON).content("""
                { "pacientesIds": [%d], "especialistasIds": [%d],
                  "data": "%s", "horaInicio": "09:00", "horaTermino": "10:00" }
                """.formatted(criarPaciente(), cenario.idEspecialista(), cenario.dataTerapia())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivoCancelamento").doesNotExist());
    }

    @Test
    void edicaoDeTerapiaPelaApi() throws Exception {
        Cenario cenario = criarCenario();
        Long novoPaciente = criarPaciente();
        String rota = "/api/terapias/" + cenario.idTerapia();

        // Validação da edição antes de salvar: a própria terapia não conflita
        String corpoVerificacao = """
                { "pacientesIds": [%d], "especialistasIds": [%d],
                  "data": "%s", "horaInicio": "09:30", "horaTermino": "10:30" %s }
                """;
        chamar(post("/api/terapias/verificar-conflitos").contentType(MediaType.APPLICATION_JSON)
                .content(corpoVerificacao.formatted(cenario.idPaciente(), cenario.idEspecialista(),
                        cenario.dataTerapia(), "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.possuiConflito").value(true));
        chamar(post("/api/terapias/verificar-conflitos").contentType(MediaType.APPLICATION_JSON)
                .content(corpoVerificacao.formatted(cenario.idPaciente(), cenario.idEspecialista(),
                        cenario.dataTerapia(), ", \"idTerapiaIgnorada\": " + cenario.idTerapia())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.possuiConflito").value(false));

        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("""
                { "horaInicio": "09:30", "horaTermino": "10:30", "pacientesIds": [%d, %d] }
                """.formatted(cenario.idPaciente(), novoPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaInicio").value("09:30"))
                .andExpect(jsonPath("$.data").value(cenario.dataTerapia().toString()))
                .andExpect(jsonPath("$.modalidade").value("COLETIVA"))
                .andExpect(jsonPath("$.pacientes.length()").value(2))
                .andExpect(jsonPath("$.pacientes[*].presenca", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("PENDENTE"))));

        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"horaInicio\": \"9h\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido ou malformado."));
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"pacientesIds\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A terapia deve ter ao menos um paciente."));
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"especialistasIds\": [null] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['especialistasIds[0]']").value("O ID do especialista não pode ser nulo"));
        chamar(patch("/api/terapias/987654").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Terapia não encontrada com ID: 987654"));

        chamar(patch(rota + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"status\": \"CANCELADA\", \"motivo\": \"Teste\" }"))
                .andExpect(status().isOk());
        chamar(patch(rota).contentType(MediaType.APPLICATION_JSON).content("{ \"horaInicio\": \"08:00\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Só é possível editar terapias agendadas. Esta terapia está cancelada."));
    }

    @Test
    void listagemDeProfissionaisFiltraPorTipoEStatusNoPostgres() throws Exception {
        Cenario cenario = criarCenario();
        String cpfEspecialista = jdbcTemplate.queryForObject(
                "select \"CPF\" from \"Pessoa\" where \"IDCadastro\" = ?", String.class, cenario.idEspecialista());
        String cpfPJ = jdbcTemplate.queryForObject(
                "select \"CPF\" from \"Pessoa\" where \"IDCadastro\" = ?", String.class, cenario.idPJ());

        chamar(get("/api/profissionais?cpf=" + cpfPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("ESPECIALISTA_PJ"));
        chamar(get("/api/profissionais?tipo=ESPECIALISTA&tipo=ESPECIALISTA_PJ&statusCadastro=ATIVO&cpf=" + cpfEspecialista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("ESPECIALISTA"));
        chamar(get("/api/profissionais?tipo=PROFISSIONAL&cpf=" + cpfEspecialista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        chamar(get("/api/profissionais?statusCadastro=INATIVO&cpf=" + cpfPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        chamar(get("/api/profissionais?tipo=XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'tipo': XYZ"));
        chamar(get("/api/profissionais?tipo=PESSOA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'tipo': PESSOA"));
        chamar(get("/api/profissionais?statusCadastro=XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'statusCadastro': XYZ"));
    }

    @Test
    void historicoDoPacientePelaApi() throws Exception {
        Cenario cenario = criarCenario();
        String rota = "/api/pacientes/" + cenario.idPaciente() + "/historico";

        chamar(patch("/api/pacientes/" + cenario.idPaciente()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"statusPaciente\": \"ALTA\", \"prontuario\": { \"alergias\": \"Dipirona\" } }"))
                .andExpect(status().isOk());
        // Operação recusada não deixa evento
        chamar(patch("/api/pacientes/" + cenario.idPaciente()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Outro nome\", \"responsaveis\": [] }"))
                .andExpect(status().isBadRequest());

        String resposta = chamar(get(rota))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].tipo").value("DADOS_ALTERADOS"))
                .andExpect(jsonPath("$.content[0].descricao").value("Dados do cadastro alterados: informações clínicas."))
                .andExpect(jsonPath("$.content[0].idTerapia").doesNotExist())
                .andExpect(jsonPath("$.content[0].usuario.nome").value("Administrador do Sistema"))
                .andExpect(jsonPath("$.content[1].tipo").value("STATUS_PACIENTE_ALTERADO"))
                .andExpect(jsonPath("$.content[2].tipo").value("TERAPIA_AGENDADA"))
                .andExpect(jsonPath("$.content[2].idTerapia").value(cenario.idTerapia()))
                .andExpect(jsonPath("$.content[3].tipo").value("RESPONSAVEL_VINCULADO"))
                .andExpect(jsonPath("$.content[4].tipo").value("CADASTRO"))
                .andReturn().getResponse().getContentAsString();
        assertThat(resposta).doesNotContain("Dipirona").doesNotContain("F84").doesNotContain("Outro nome");
        assertThat((String) JsonPath.read(resposta, "$.content[0].dataHora")).endsWith("Z");

        chamar(get(rota + "?tipo=CADASTRO&tipo=TERAPIA_AGENDADA&inicio=2000-01-01&fim=2999-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
        chamar(get(rota + "?sort=dataHora,asc&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("CADASTRO"))
                .andExpect(jsonPath("$.page.totalPages").value(5));

        chamar(get(rota + "?tipo=XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'tipo': XYZ"));
        chamar(get(rota + "?inicio=2026-10-10&fim=2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A data final deve ser igual ou posterior à data inicial."));
        chamar(get("/api/pacientes/987654/historico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Paciente não encontrado com ID: 987654"));

        // Consulta comum: não vai para a auditoria
        Integer auditados = jdbcTemplate.queryForObject(
                "select count(*) from \"LogAuditoria\" where \"Caminho\" = ?", Integer.class, rota);
        assertThat(auditados).isZero();
    }

    // Sem CORS_ORIGINS, só o front local e o publicado no GitHub Pages são liberados
    @Test
    void corsPadraoLiberaSoOsFrontsConhecidos() throws Exception {
        mockMvc.perform(options("/api/terapias")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://eduardofproenca.github.io")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://eduardofproenca.github.io"));
        mockMvc.perform(options("/api/terapias")
                        .header("Origin", "https://outro.exemplo.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioLogadoIdentificaOAdministradorSemAuditar() throws Exception {
        Integer registrosAntes = jdbcTemplate.queryForObject(
                "select count(*) from \"LogAuditoria\" where \"Caminho\" = '/api/usuarios/me'", Integer.class);

        chamar(get("/api/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Administrador do Sistema"))
                .andExpect(jsonPath("$.cpf").value(CPF_ADMIN))
                .andExpect(jsonPath("$.tipoCadastro").value("PESSOA"))
                .andExpect(jsonPath("$.perfil.nome").value("Administrador"))
                .andExpect(jsonPath("$.permissoes[0].nome").value("ACESSO_TOTAL"))
                .andExpect(jsonPath("$.permissoes.length()").value(1));

        Integer registrosDepois = jdbcTemplate.queryForObject(
                "select count(*) from \"LogAuditoria\" where \"Caminho\" = '/api/usuarios/me'", Integer.class);
        assertThat(registrosDepois).isEqualTo(registrosAntes);
    }

    // ---------- Permissões dos perfis padrão (V15) ----------

    @Test
    void recepcaoEditaPacientesEAgendaMasNaoCadastraNemInativa() throws Exception {
        Cenario cenario = criarCenario();
        String recepcao = tokenComPerfil("Recepção");
        String paciente = "/api/pacientes/" + cenario.idPaciente();

        assertThat(statusCom(recepcao, get("/api/pacientes?termo=Paciente"))).isEqualTo(200);
        assertThat(statusCom(recepcao, json(patch(paciente), "{ \"nome\": \"Paciente Editado\" }"))).isEqualTo(200);
        assertThat(statusCom(recepcao, json(post("/api/terapias"),
                corpoTerapia(cenario.idPaciente(), cenario.idEspecialista())))).isEqualTo(201);
        // Lista especialistas para agendar, mas não abre o cadastro deles
        assertThat(statusCom(recepcao, get("/api/profissionais"))).isEqualTo(200);
        assertThat(statusCom(recepcao, get("/api/escolas/sugestoes"))).isEqualTo(200);

        mockMvc.perform(json(post("/api/pacientes"), corpoPaciente(cpfNovo(), null))
                        .header("Authorization", "Bearer " + recepcao))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem permissão para realizar esta ação."));
        for (MockHttpServletRequestBuilder proibida : List.of(
                delete(paciente), get("/api/profissionais/" + cenario.idEspecialista()), get("/api/auditoria"),
                get("/api/perfis"), get("/api/usuarios"), get("/api/escolas"), get("/api/cargos/1"))) {
            assertThat(statusCom(recepcao, proibida)).as(proibida.toString()).isEqualTo(403);
        }

        // A tentativa recusada também fica na auditoria
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from "LogAuditoria"
                where "Metodo" = 'DELETE' and "Caminho" = ? and "StatusHttp" = 403
                """, Integer.class, paciente)).isEqualTo(1);
    }

    @Test
    void especialistaVeSoAsTerapiasDeQueParticipa() throws Exception {
        Cenario seu = criarCenario();
        Cenario outro = criarCenario();
        String cpf = jdbcTemplate.queryForObject(
                "select \"CPF\" from \"Pessoa\" where \"IDCadastro\" = ?", String.class, seu.idEspecialista());
        String especialista = concederAcesso("/api/especialistas/" + seu.idEspecialista(), cpf, "Especialista");

        assertThat(statusCom(especialista, get("/api/terapias/" + seu.idTerapia()))).isEqualTo(200);
        assertThat(statusCom(especialista, get("/api/terapias/" + outro.idTerapia()))).isEqualTo(403);
        assertThat(statusCom(especialista, get("/api/terapias/999999"))).isEqualTo(403);
        assertThat(statusCom(especialista, get("/api/terapias"))).isEqualTo(403);

        mockMvc.perform(get("/api/terapias/minhas?idPaciente=" + seu.idPaciente())
                        .header("Authorization", "Bearer " + especialista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(get("/api/terapias/minhas?idPaciente=" + outro.idPaciente())
                        .header("Authorization", "Bearer " + especialista))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        assertThat(statusCom(especialista, get("/api/pacientes/" + seu.idPaciente() + "/historico"))).isEqualTo(200);
        assertThat(statusCom(especialista, json(patch("/api/terapias/" + seu.idTerapia() + "/status"),
                "{ \"status\": \"EM_ATENDIMENTO\" }"))).isEqualTo(403);
        assertThat(statusCom(especialista, json(post("/api/terapias"),
                corpoTerapia(seu.idPaciente(), seu.idEspecialista())))).isEqualTo(403);
    }

    // V18: o perfil "Assistente Social" só lê os pacientes vinculados a ele (LGPD)
    @Test
    void assistenteSocialSoLeOsPacientesVinculados() throws Exception {
        Cenario cenario = criarCenario();
        Long naoVinculado = criarPaciente();
        String cpf = cpfNovo();
        Long idAssistente = criar("/api/assistentes-sociais", """
                { "nome": "Assistente %1$s", "cpf": "%1$s", "dataNascimento": "1988-04-12", "sexo": "F",
                  "estadoCivil": "CASADO", "enderecos": [], "email": "as%1$s@email.com", "idCargo": 1,
                  "registroConselho": "CRESS-9/%1$s", "idsPacientes": [%2$d] }
                """.formatted(cpf, cenario.idPaciente()));
        String assistente = concederAcesso("/api/assistentes-sociais/" + idAssistente, cpf, "Assistente Social");

        for (String permitida : List.of("/api/pacientes/" + cenario.idPaciente(),
                "/api/pacientes/" + cenario.idPaciente() + "/historico", "/api/assistentes-sociais/me/pacientes",
                "/api/especialidades")) {
            assertThat(statusCom(assistente, get(permitida))).as(permitida).isEqualTo(200);
        }
        // A busca traz só os vinculados
        mockMvc.perform(get("/api/pacientes?termo=Paciente").header("Authorization", "Bearer " + assistente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idCadastro", org.hamcrest.Matchers.contains(cenario.idPaciente().intValue())));
        for (MockHttpServletRequestBuilder proibida : List.of(
                get("/api/pacientes/" + naoVinculado), get("/api/pacientes/" + naoVinculado + "/historico"),
                get("/api/responsaveis"), get("/api/terapias"), get("/api/profissionais"), get("/api/cids/F84.0"),
                json(patch("/api/pacientes/" + cenario.idPaciente()), "{ \"nome\": \"Outro\" }"))) {
            assertThat(statusCom(assistente, proibida)).as(proibida.toString()).isEqualTo(403);
        }
    }

    // ---------- Escolas ----------

    @Test
    void gestaoDeEscolas() throws Exception {
        String nome = "Escola Gestão " + SEQUENCIA.incrementAndGet();
        Long idEscola = criar("/api/escolas", "{ \"nome\": \"%s\", \"tipoEscola\": \"PUBLICA\" }".formatted(nome),
                "$.idEscola");
        String rota = "/api/escolas/" + idEscola;

        chamar(get("/api/escolas?nome=" + nome.toUpperCase() + "&tipoEscola=PUBLICA"))
                .andExpect(jsonPath("$.content[0].idEscola").value(idEscola))
                .andExpect(jsonPath("$.content[0].quantidadePacientes").value(0));
        chamar(get("/api/escolas?nome=" + nome + "&tipoEscola=PARTICULAR"))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        chamar(json(patch(rota), "{ \"tipoEscola\": \"PARTICULAR\", \"telefone\": \"1533334444\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEscola").value("PARTICULAR"))
                .andExpect(jsonPath("$.nome").value(nome));
        chamar(json(patch(rota), "{ \"telefone\": null }")).andExpect(jsonPath("$.telefone").doesNotExist());
        chamar(json(patch(rota), "{ \"nome\": \" \" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O nome da escola não pode ser vazio."));
        chamar(json(patch(rota), "{ \"tipoEscola\": null }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O tipo da escola não pode ser nulo."));

        // Com paciente vinculado (mesmo inativo), a exclusão é recusada
        Long idPaciente = criar("/api/pacientes", corpoPaciente(cpfNovo(), idEscola));
        chamar(delete("/api/pacientes/" + idPaciente)).andExpect(status().isOk());
        chamar(get(rota)).andExpect(jsonPath("$.quantidadePacientes").value(1));
        chamar(get("/api/pacientes?idEscola=" + idEscola))
                .andExpect(jsonPath("$[0].idCadastro").value(idPaciente))
                .andExpect(jsonPath("$.length()").value(1));
        chamar(delete(rota))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não é possível excluir a escola: há pacientes vinculados a ela."));

        Long semPacientes = criar("/api/escolas", "{ \"nome\": \"Escola Vazia\", \"tipoEscola\": \"PUBLICA\" }",
                "$.idEscola");
        chamar(delete("/api/escolas/" + semPacientes)).andExpect(status().isNoContent());
        chamar(get("/api/escolas/" + semPacientes))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Escola não encontrada com ID: " + semPacientes));
    }

    // ---------- Especialidades ----------

    @Test
    void gestaoDeEspecialidades() throws Exception {
        String nome = "Musicoterapia " + SEQUENCIA.incrementAndGet();
        Long idEspecialidade = criar("/api/especialidades",
                "{ \"nome\": \"%s\", \"descricao\": \"Terapia com música\" }".formatted(nome), "$.idEspecialidade");
        String rota = "/api/especialidades/" + idEspecialidade;

        Long idEspecialista = criar("/api/especialistas", """
                { "nome": "Especialista %1$s", "cpf": "%1$s", "dataNascimento": "1985-08-20", "sexo": "F",
                  "estadoCivil": "CASADO", "email": "esp%1$s@email.com", "idCargo": 1,
                  "registroConselho": "CRP-%1$s", "especialidadesIds": [%2$d],
                  "jornadas": [ { "diaSemana": 2, "horaInicio": "08:00", "horaTermino": "12:00" } ] }
                """.formatted(cpfNovo(), idEspecialidade));

        chamar(get("/api/especialidades"))
                .andExpect(jsonPath("$[?(@.idEspecialidade == %d)].quantidadeEspecialistas".formatted(idEspecialidade))
                        .value(1));
        chamar(get(rota))
                .andExpect(jsonPath("$.quantidadeEspecialistas").value(1))
                .andExpect(jsonPath("$.especialistas[0].idCadastro").value(idEspecialista))
                .andExpect(jsonPath("$.especialistas[0].tipo").value("ESPECIALISTA"))
                .andExpect(jsonPath("$.especialistas[0].statusCadastro").value("ATIVO"));

        chamar(json(patch(rota), "{ \"nome\": \"psicologia\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Já existe uma especialidade com esse nome."));
        chamar(json(patch(rota), "{ \"nome\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O nome da especialidade não pode ser vazio."));
        chamar(json(patch(rota), "{ \"nome\": \"%s\", \"descricao\": null }".formatted(nome.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(nome.toUpperCase()))
                .andExpect(jsonPath("$.descricao").doesNotExist());

        chamar(delete(rota))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Não é possível excluir a especialidade: há especialistas vinculados a ela."));

        Long semEspecialistas = criar("/api/especialidades",
                "{ \"nome\": \"Equoterapia %d\" }".formatted(SEQUENCIA.incrementAndGet()), "$.idEspecialidade");
        chamar(delete("/api/especialidades/" + semEspecialistas)).andExpect(status().isNoContent());
        chamar(get("/api/especialidades/" + semEspecialistas))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Especialidade não encontrada com ID: " + semEspecialistas));
    }

    private record Cenario(Long idPaciente, String cpfPaciente, Long idResponsavel, Long idEspecialista,
            Long idPJ, Long idTerapia, LocalDate dataTerapia) {
    }

    private Cenario criarCenario() throws Exception {
        String cpfPaciente = cpfNovo();
        Long idPaciente = criarPaciente(cpfPaciente);
        String detalhe = chamar(get("/api/pacientes/" + idPaciente)).andReturn().getResponse().getContentAsString();
        Long idResponsavel = ((Number) JsonPath.read(detalhe, "$.responsaveis[0].responsavel.idCadastro")).longValue();

        Long idEspecialista = criar("/api/especialistas", """
                { "nome": "Especialista %1$s", "cpf": "%1$s", "dataNascimento": "1985-08-20", "sexo": "F",
                  "estadoCivil": "CASADO", "email": "esp%1$s@email.com", "idCargo": 1,
                  "registroConselho": "CRP-%1$s", "especialidadesIds": [1],
                  "jornadas": [ { "diaSemana": 1, "horaInicio": "08:00", "horaTermino": "17:00" } ] }
                """.formatted(cpfNovo()));

        String cpfPJ = cpfNovo();
        Long idPJ = criar("/api/especialistas/pj", """
                { "nome": "PJ %1$s", "cpf": "%1$s", "dataNascimento": "1982-03-15", "sexo": "F",
                  "estadoCivil": "SOLTEIRO", "email": "pj%1$s@email.com", "idCargo": 1,
                  "registroConselho": "CRM-%1$s", "especialidadesIds": [2],
                  "jornadas": [ { "diaSemana": 5, "horaInicio": "08:00", "horaTermino": "16:00" } ],
                  "cnpj": "%2$s", "razaoSocial": "Clínica %1$s Ltda" }
                """.formatted(cpfPJ, "000" + cpfPJ));

        // Uma segunda-feira diferente por cenário, dentro da jornada do especialista (diaSemana 1)
        LocalDate data = LocalDate.of(2099, 1, 1).plusWeeks(SEQUENCIA.incrementAndGet())
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        Long idTerapia = criar("/api/terapias", """
                { "pacientesIds": [%d], "especialistasIds": [%d],
                  "data": "%s", "horaInicio": "09:00", "horaTermino": "10:00" }
                """.formatted(idPaciente, idEspecialista, data), "$.idTerapia");

        return new Cenario(idPaciente, cpfPaciente, idResponsavel, idEspecialista, idPJ, idTerapia, data);
    }

    private Long criarPaciente() throws Exception {
        return criarPaciente(cpfNovo());
    }

    private Long criarPaciente(String cpf) throws Exception {
        return criar("/api/pacientes", corpoPaciente(cpf, null));
    }

    private String corpoPaciente(String cpf, Long idEscola) {
        return """
                { "nome": "Paciente %1$s", "cpf": "%1$s", "dataNascimento": "2015-03-21", "sexo": "M",
                  "estadoCivil": "SOLTEIRO", "idEscola": %3$s,
                  "enderecos": [ { "cep": "18010000", "rua": "Rua das Flores", "numero": "%1$s",
                                   "bairro": "Centro", "cidade": "Sorocaba", "estado": "SP" } ],
                  "novosCids": [ { "codigoCID": "F84.0", "descricao": "Autismo infantil" } ],
                  "prontuario": {},
                  "responsaveis": [ { "nome": "Responsável %2$s", "cpf": "%2$s", "dataNascimento": "1988-07-02",
                                      "sexo": "F", "estadoCivil": "CASADO", "grauParentesco": "MAE",
                                      "naoResideComPaciente": false } ] }
                """.formatted(cpf, cpfNovo(), idEscola);
    }

    // Profissional novo com acesso no perfil padrão informado (criado pela migration V15)
    private String tokenComPerfil(String perfil) throws Exception {
        String cpf = cpfNovo();
        Long idProfissional = criar("/api/profissionais", """
                { "nome": "Profissional %1$s", "cpf": "%1$s", "dataNascimento": "1990-05-10", "sexo": "F",
                  "estadoCivil": "SOLTEIRO", "email": "prof%1$s@email.com", "idCargo": 1 }
                """.formatted(cpf));
        return concederAcesso("/api/profissionais/" + idProfissional, cpf, perfil);
    }

    private String concederAcesso(String rotaDoCadastro, String cpf, String perfil) throws Exception {
        Long idPerfil = jdbcTemplate.queryForObject(
                "select \"IDPerfil\" from \"Perfil\" where \"Nome\" = ?", Long.class, perfil);
        chamar(post(rotaDoCadastro + "/usuario").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"idPerfil\": %d }".formatted(idPerfil)))
                .andExpect(status().isCreated());
        // O acesso vem com senha temporária enviada por e-mail: o teste grava uma senha definitiva
        // conhecida para entrar direto, sem passar pelo primeiro acesso
        jdbcTemplate.update("""
                update "Usuario" set "Senha" = ?, "SenhaTemporaria" = false
                where "IDCadastro" = (select "IDCadastro" from "Pessoa" where "CPF" = ?)
                """, passwordEncoder.encode("SenhaTeste123"), cpf);
        String email = jdbcTemplate.queryForObject(
                "select \"Email\" from \"Pessoa\" where \"CPF\" = ?", String.class, cpf);
        String resposta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"senha\": \"SenhaTeste123\" }".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    // Status de cada requisição feita com o token; corpo JSON quando informado
    private int statusCom(String tokenDoUsuario, MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header("Authorization", "Bearer " + tokenDoUsuario))
                .andReturn().getResponse().getStatus();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private String corpoTerapia(Long idPaciente, Long idEspecialista) {
        return """
                { "pacientesIds": [%d], "especialistasIds": [%d],
                  "data": "%s", "horaInicio": "14:00", "horaTermino": "15:00" }
                """.formatted(idPaciente, idEspecialista, LocalDate.of(2099, 6, 1).plusWeeks(SEQUENCIA.incrementAndGet())
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)));
    }

    private void criarEscola() throws Exception {
        chamar(post("/api/escolas").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Escola Teste\", \"tipoEscola\": \"PUBLICA\" }"))
                .andExpect(status().isCreated());
    }

    private Long criar(String rota, String corpo) throws Exception {
        return criar(rota, corpo, "$.idCadastro");
    }

    private Long criar(String rota, String corpo, String caminhoDoId) throws Exception {
        String resposta = chamar(post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, caminhoDoId)).longValue();
    }

    private ResultActions chamar(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header("Authorization", "Bearer " + token));
    }

    // CPF válido (passa no @CpfValido): base sequencial com os dígitos verificadores certos
    private static String cpfNovo() {
        String base = "%09d".formatted(700000000L + SEQUENCIA.incrementAndGet());
        for (int verificadores = 0; verificadores < 100; verificadores++) {
            String cpf = base + "%02d".formatted(verificadores);
            if (CpfValidator.valido(cpf)) {
                return cpf;
            }
        }
        throw new IllegalStateException("Sem dígitos verificadores para " + base);
    }

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível iniciar o PostgreSQL embarcado", e);
        }
    }
}
