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
import java.time.LocalDate;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

    private String token;

    @BeforeEach
    void entrarComoAdministrador() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"cpf\": \"" + CPF_ADMIN + "\", \"senha\": \"Admin@12345\" }"))
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

        assertThat(ultimaVersao).isEqualTo(14);
        assertThat(indices).contains(
                "idx_pessoa_nome_trgm", "idx_pessoa_cpf_trgm", "idx_escola_nome_trgm", "idx_cbo_titulo_trgm",
                "idx_terapia_status_data_inicio", "idx_especialista_terapia_especialista",
                "idx_paciente_terapia_terapia", "idx_log_auditoria_data_hora", "idx_evento_paciente_data_hora");
    }

    // Cria um cenário completo pela API e consulta todas as listagens e detalhes
    @Test
    void listagensEDetalhesFuncionamSemOpenInView() throws Exception {
        Cenario cenario = criarCenario();

        for (String rota : List.of(
                "/api/pacientes", "/api/pacientes?nome=paciente&cpf=" + cenario.cpfPaciente().substring(0, 5),
                "/api/pacientes/" + cenario.idPaciente(),
                "/api/responsaveis", "/api/responsaveis/" + cenario.idResponsavel(),
                "/api/profissionais", "/api/profissionais/" + cenario.idEspecialista(),
                "/api/especialistas/" + cenario.idEspecialista(), "/api/especialistas/pj/" + cenario.idPJ(),
                "/api/especialidades", "/api/cargos", "/api/cargos/1", "/api/cbos?titulo=teste", "/api/cbos/999999",
                "/api/perfis", "/api/perfis/1", "/api/permissoes",
                "/api/terapias", "/api/terapias?status=AGENDADA", "/api/terapias/" + cenario.idTerapia(),
                "/api/escolas", "/api/escolas?nome=escola", "/api/cids/F84.0", "/api/auditoria")) {
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

        chamar(get("/api/escolas")).andExpect(status().isOk()).andExpect(jsonPath("$[0].idEscola").exists());
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
                .andExpect(status().isBadRequest())
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
                .content("{ \"cpf\": \"" + CPF_ADMIN + "\", \"senha\": \"errada\" }"))
                .andExpect(status().isUnauthorized());
        // Listagens comuns não são auditadas
        chamar(get("/api/pacientes")).andExpect(status().isOk());

        List<String> registros = jdbcTemplate.queryForList("""
                select "Acao" || ' ' || "Metodo" || ' ' || "Caminho" || ' ' || "StatusHttp" || ' ' || coalesce("CPFUsuario", '-')
                from "LogAuditoria" order by "IDLog"
                """, String.class);

        assertThat(registros).contains(
                "CRIACAO POST /api/pacientes 201 " + CPF_ADMIN,
                "CONSULTA GET /api/pacientes/" + idPaciente + " 200 " + CPF_ADMIN,
                "LOGIN POST /api/auth/login 401 " + CPF_ADMIN,
                "LOGIN POST /api/auth/login 200 " + CPF_ADMIN);
        assertThat(registros).doesNotContain("CONSULTA GET /api/pacientes 200 " + CPF_ADMIN);

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

    // Sem CORS_ORIGINS, só o front local é liberado
    @Test
    void corsPadraoLiberaSoOFrontLocal() throws Exception {
        mockMvc.perform(options("/api/terapias")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
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
                .andExpect(jsonPath("$.permissoes.length()").value(5));

        Integer registrosDepois = jdbcTemplate.queryForObject(
                "select count(*) from \"LogAuditoria\" where \"Caminho\" = '/api/usuarios/me'", Integer.class);
        assertThat(registrosDepois).isEqualTo(registrosAntes);
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

        LocalDate data = LocalDate.of(2027, 1, 1).plusDays(SEQUENCIA.incrementAndGet());
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
        return criar("/api/pacientes", """
                { "nome": "Paciente %1$s", "cpf": "%1$s", "dataNascimento": "2015-03-21", "sexo": "M",
                  "estadoCivil": "SOLTEIRO",
                  "enderecos": [ { "cep": "18010000", "rua": "Rua das Flores", "numero": "%1$s",
                                   "bairro": "Centro", "cidade": "Sorocaba", "estado": "SP" } ],
                  "novosCids": [ { "codigoCID": "F84.0", "descricao": "Autismo infantil" } ],
                  "prontuario": {},
                  "responsaveis": [ { "nome": "Responsável %2$s", "cpf": "%2$s", "dataNascimento": "1988-07-02",
                                      "sexo": "F", "estadoCivil": "CASADO", "grauParentesco": "MAE",
                                      "naoResideComPaciente": false } ] }
                """.formatted(cpf, cpfNovo()));
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

    private static String cpfNovo() {
        return "%011d".formatted(70000000000L + SEQUENCIA.incrementAndGet());
    }

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível iniciar o PostgreSQL embarcado", e);
        }
    }
}
