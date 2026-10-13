package com.fatec.gisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        assertThat(ultimaVersao).isEqualTo(12);
        assertThat(indices).contains(
                "idx_pessoa_nome_trgm", "idx_pessoa_cpf_trgm", "idx_escola_nome_trgm", "idx_cbo_titulo_trgm",
                "idx_terapia_status_data_inicio", "idx_especialista_terapia_especialista",
                "idx_paciente_terapia_terapia", "idx_log_auditoria_data_hora");
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
