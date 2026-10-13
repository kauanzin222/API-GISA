package com.fatec.gisa.services.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.request.PacientePatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ProntuarioPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelVinculoPatchDTO;
import com.fatec.gisa.dtos.paciente.response.EventoPacienteDTO;
import com.fatec.gisa.dtos.terapia.request.PresencaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.StatusTerapiaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaPatchRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.paciente.Responsavel;
import com.fatec.gisa.entities.paciente.VinculoResponsavel;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.GrauParentesco;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.enums.StatusPresenca;
import com.fatec.gisa.enums.StatusTerapia;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.services.terapia.TerapiaService;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:historico-paciente;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Transactional
class HistoricoPacienteTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 27);
    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired
    private TerapiaService terapiaService;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private HistoricoPacienteService historicoPacienteService;

    @Autowired
    private EntityManager entityManager;

    private Cargo cargo;
    private Especialista ana;
    private Especialista carlos;
    private Paciente lucas;
    private Paciente maria;
    private Responsavel mae;

    @BeforeEach
    void preparar() {
        Cbo cbo = new Cbo("999999", "Teste");
        entityManager.persist(cbo);
        cargo = new Cargo(null, "Terapeuta", cbo);
        entityManager.persist(cargo);

        ana = especialista("Ana Souza", "10000000001");
        carlos = especialista("Carlos Lima", "10000000002");
        mae = responsavel("Rosa Martins", "30000000001");
        lucas = paciente("Lucas Martins", "20000000001", mae);
        maria = paciente("Maria Costa", "20000000002", null);
        entityManager.flush();

        // Quem faz as ações nos testes
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ana.getCpf(), null, List.of()));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    // ---------- Terapias ----------

    @Test
    void terapiaColetivaEntraNoHistoricoDeCadaPacienteComOAutor() {
        Long idTerapia = cadastrar(DATA, List.of(lucas, maria), List.of(ana, carlos), "09:00", "10:00");

        for (Paciente paciente : List.of(lucas, maria)) {
            EventoPacienteDTO evento = unico(paciente);
            assertThat(evento.tipo()).isEqualTo(TipoEventoPaciente.TERAPIA_AGENDADA);
            assertThat(evento.descricao()).isEqualTo(
                    "Terapia coletiva agendada para 27/10/2026, das 09:00 às 10:00, com Ana Souza e Carlos Lima.");
            assertThat(evento.idTerapia()).isEqualTo(idTerapia);
            assertThat(evento.usuario().idCadastro()).isEqualTo(ana.getIdCadastro());
            assertThat(evento.usuario().nome()).isEqualTo("Ana Souza");
            assertThat(evento.dataHora()).isNotNull();
        }
    }

    @Test
    void terapiaIndividualUsaODescritivoSimples() {
        cadastrar(DATA, List.of(lucas), List.of(ana), "09:00", "10:00");

        assertThat(unico(lucas).descricao())
                .isEqualTo("Terapia agendada para 27/10/2026, das 09:00 às 10:00, com Ana Souza.");
        assertThat(eventos(maria)).isEmpty();
    }

    @Test
    void presencaEntraSoNoHistoricoDoPacienteMarcadoESemAJustificativa() {
        Long idTerapia = cadastrar(HOJE, List.of(lucas, maria), List.of(ana), "09:00", "10:00");

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Internação por crise asmática"));
        // Reenviar a mesma marcação não gera evento novo
        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Outro texto"));

        List<EventoPacienteDTO> doLucas = eventos(lucas, TipoEventoPaciente.PRESENCA_REGISTRADA);
        assertThat(doLucas).singleElement().satisfies(evento -> {
            assertThat(evento.descricao()).isEqualTo("Presença registrada na terapia de "
                    + TextoHistorico.horario(HOJE.atTime(9, 0), HOJE.atTime(10, 0)) + ": falta justificada.");
            assertThat(evento.descricao()).doesNotContain("asmática");
            assertThat(evento.idTerapia()).isEqualTo(idTerapia);
        });
        assertThat(eventos(maria, TipoEventoPaciente.PRESENCA_REGISTRADA)).isEmpty();
    }

    @Test
    void cancelamentoEntraNoHistoricoDeTodosSemOMotivo() {
        Long idTerapia = cadastrar(DATA, List.of(lucas, maria), List.of(ana), "09:00", "10:00");

        terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.EM_ATENDIMENTO, null));
        terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.CANCELADA, "Paciente com febre"));

        for (Paciente paciente : List.of(lucas, maria)) {
            // Iniciar o atendimento não gera evento; cancelar, sim
            assertThat(eventos(paciente)).extracting(EventoPacienteDTO::tipo)
                    .containsExactly(TipoEventoPaciente.TERAPIA_CANCELADA, TipoEventoPaciente.TERAPIA_AGENDADA);
            assertThat(eventos(paciente).get(0).descricao())
                    .isEqualTo("Terapia de 27/10/2026, das 09:00 às 10:00, com Ana Souza, cancelada.")
                    .doesNotContain("febre");
        }
    }

    @Test
    void edicaoRegistraQuemContinuaQuemEntraEQuemSai() {
        Paciente bia = paciente("Beatriz Lima", "20000000003", null);
        Long idTerapia = cadastrar(DATA, List.of(lucas, maria), List.of(ana), "09:00", "10:00");

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setHoraInicio(LocalTime.of(11, 0));
        dto.setHoraTermino(LocalTime.of(12, 0));
        dto.setPacientesIds(List.of(lucas.getIdCadastro(), bia.getIdCadastro()));
        dto.setEspecialistasIds(List.of(carlos.getIdCadastro()));
        terapiaService.atualizar(idTerapia, dto);

        assertThat(eventos(lucas).get(0)).satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo(TipoEventoPaciente.TERAPIA_ALTERADA);
            assertThat(evento.descricao()).isEqualTo("Terapia de 27/10/2026, das 09:00 às 10:00, alterada: "
                    + "remarcada para 27/10/2026, das 11:00 às 12:00; especialistas: Carlos Lima; "
                    + "pacientes da terapia alterados.");
            // O nome do outro paciente não aparece no histórico de quem continua
            assertThat(evento.descricao()).doesNotContain("Beatriz").doesNotContain("Maria");
        });
        assertThat(unico(bia)).satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo(TipoEventoPaciente.TERAPIA_AGENDADA);
            assertThat(evento.descricao())
                    .isEqualTo("Paciente incluído na terapia de 27/10/2026, das 11:00 às 12:00, com Carlos Lima.");
        });
        assertThat(eventos(maria).get(0)).satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo(TipoEventoPaciente.TERAPIA_ALTERADA);
            assertThat(evento.descricao())
                    .isEqualTo("Paciente retirado da terapia de 27/10/2026, das 09:00 às 10:00, com Ana Souza.");
            assertThat(evento.idTerapia()).isEqualTo(idTerapia);
        });
    }

    @Test
    void edicaoQueMudaSoAModalidadeEEdicaoSemMudancas() {
        Long idTerapia = cadastrar(DATA, List.of(lucas), List.of(ana), "09:00", "10:00");

        terapiaService.atualizar(idTerapia, new TerapiaPatchRequestDTO());
        assertThat(eventos(lucas)).hasSize(1);

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setPacientesIds(List.of(lucas.getIdCadastro(), maria.getIdCadastro()));
        terapiaService.atualizar(idTerapia, dto);
        assertThat(eventos(lucas).get(0).descricao())
                .isEqualTo("Terapia de 27/10/2026, das 09:00 às 10:00, alterada: agora coletiva.");
    }

    // ---------- Paciente ----------

    @Test
    void patchRegistraSoOQueMudouSemValoresClinicos() {
        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setNome("Lucas Martins Silva");
        dto.setCelular(lucas.getCelular()); // mesmo valor: não conta
        dto.setStatusPaciente(StatusPaciente.ALTA);
        ProntuarioPatchRequestDTO prontuario = new ProntuarioPatchRequestDTO();
        prontuario.setAlergias("Amendoim");
        dto.setProntuario(prontuario);
        pacienteService.atualizar(lucas.getIdCadastro(), dto);

        List<EventoPacienteDTO> eventos = eventos(lucas);
        assertThat(eventos).extracting(EventoPacienteDTO::tipo).containsExactly(
                TipoEventoPaciente.DADOS_ALTERADOS, TipoEventoPaciente.STATUS_PACIENTE_ALTERADO);
        assertThat(eventos.get(0).descricao())
                .isEqualTo("Dados do cadastro alterados: nome e informações clínicas.")
                .doesNotContain("Amendoim");
        assertThat(eventos.get(1).descricao()).isEqualTo("Status do paciente alterado de Matriculado para Alta.");
        assertThat(eventos).extracting(EventoPacienteDTO::idTerapia).containsOnlyNulls();

        // Reenviar os mesmos valores não gera evento
        PacientePatchRequestDTO igual = new PacientePatchRequestDTO();
        igual.setNome("Lucas Martins Silva");
        igual.setStatusPaciente(StatusPaciente.ALTA);
        pacienteService.atualizar(lucas.getIdCadastro(), igual);
        assertThat(eventos(lucas)).hasSize(2);
    }

    @Test
    void inativacaoEReativacao() {
        pacienteService.inativar(lucas.getIdCadastro());
        // Inativar de novo não muda nada
        pacienteService.inativar(lucas.getIdCadastro());

        assertThat(eventos(lucas)).extracting(EventoPacienteDTO::tipo, EventoPacienteDTO::descricao).containsExactly(
                org.assertj.core.groups.Tuple.tuple(TipoEventoPaciente.STATUS_PACIENTE_ALTERADO,
                        "Status do paciente alterado de Matriculado para Desligado."),
                org.assertj.core.groups.Tuple.tuple(TipoEventoPaciente.INATIVADO, "Cadastro inativado."));

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setStatusCadastro(StatusCadastro.ATIVO);
        pacienteService.atualizar(lucas.getIdCadastro(), dto);
        assertThat(eventos(lucas).get(0).tipo()).isEqualTo(TipoEventoPaciente.REATIVADO);
    }

    @Test
    void trocaDeResponsaveisNoPatch() {
        Responsavel pai = responsavel("Paulo Martins", "30000000002");
        entityManager.flush();

        PacientePatchRequestDTO dto = new PacientePatchRequestDTO();
        dto.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.PAI)));
        pacienteService.atualizar(lucas.getIdCadastro(), dto);

        assertThat(eventos(lucas)).extracting(EventoPacienteDTO::tipo, EventoPacienteDTO::descricao)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(TipoEventoPaciente.RESPONSAVEL_VINCULADO,
                                "Responsável Paulo Martins (pai) vinculado."),
                        org.assertj.core.groups.Tuple.tuple(TipoEventoPaciente.RESPONSAVEL_DESVINCULADO,
                                "Responsável Rosa Martins (mãe) desvinculado."));

        PacientePatchRequestDTO grau = new PacientePatchRequestDTO();
        grau.setResponsaveis(List.of(new ResponsavelVinculoPatchDTO(pai.getIdCadastro(), GrauParentesco.TUTOR_LEGAL)));
        pacienteService.atualizar(lucas.getIdCadastro(), grau);
        assertThat(eventos(lucas).get(0).descricao())
                .isEqualTo("Dados do cadastro alterados: parentesco de Paulo Martins.");
    }

    // ---------- Consulta ----------

    @Test
    void filtraPorTipoEPeriodoEOrdenaDoMaisRecente() {
        cadastrar(DATA, List.of(lucas), List.of(ana), "09:00", "10:00");
        pacienteService.inativar(lucas.getIdCadastro());

        assertThat(eventos(lucas)).extracting(EventoPacienteDTO::tipo).containsExactly(
                TipoEventoPaciente.STATUS_PACIENTE_ALTERADO, TipoEventoPaciente.INATIVADO,
                TipoEventoPaciente.TERAPIA_AGENDADA);
        assertThat(eventos(lucas, TipoEventoPaciente.TERAPIA_AGENDADA, TipoEventoPaciente.INATIVADO))
                .extracting(EventoPacienteDTO::tipo)
                .containsExactly(TipoEventoPaciente.INATIVADO, TipoEventoPaciente.TERAPIA_AGENDADA);

        // O período é o dia em que o evento foi gravado (hoje), e não o dia da terapia
        assertThat(historicoPacienteService.listar(lucas.getIdCadastro(), null, HOJE, HOJE, pagina())
                .getTotalElements()).isEqualTo(3);
        assertThat(historicoPacienteService.listar(lucas.getIdCadastro(), null, HOJE.plusDays(1), null, pagina())
                .getTotalElements()).isZero();
        assertThat(historicoPacienteService.listar(lucas.getIdCadastro(), null, null, HOJE.minusDays(1), pagina())
                .getTotalElements()).isZero();
    }

    @Test
    void recusaPeriodoInvertidoEPacienteInexistente() {
        assertThatThrownBy(() -> historicoPacienteService.listar(lucas.getIdCadastro(), null,
                HOJE, HOJE.minusDays(1), pagina()))
                .hasMessage("A data final deve ser igual ou posterior à data inicial.");
        assertThatThrownBy(() -> historicoPacienteService.listar(9999L, null, null, null, pagina()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Paciente não encontrado com ID: 9999");
        // Outra pessoa (não paciente) também não tem histórico
        assertThatThrownBy(() -> historicoPacienteService.listar(ana.getIdCadastro(), null, null, null, pagina()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void semUsuarioLogadoOAutorFicaNulo() {
        SecurityContextHolder.clearContext();
        cadastrar(DATA, List.of(lucas), List.of(ana), "09:00", "10:00");

        assertThat(unico(lucas).usuario()).isNull();
    }

    private Long cadastrar(LocalDate data, List<Paciente> pacientes, List<Especialista> especialistas,
            String inicio, String termino) {
        return terapiaService.cadastrar(new TerapiaCadastroRequestDTO(
                pacientes.stream().map(Paciente::getIdCadastro).toList(),
                especialistas.stream().map(Especialista::getIdCadastro).toList(),
                data, LocalTime.parse(inicio), LocalTime.parse(termino))).getIdTerapia();
    }

    private EventoPacienteDTO unico(Paciente paciente) {
        List<EventoPacienteDTO> eventos = eventos(paciente);
        assertThat(eventos).hasSize(1);
        return eventos.get(0);
    }

    private List<EventoPacienteDTO> eventos(Paciente paciente, TipoEventoPaciente... tipos) {
        return historicoPacienteService.listar(paciente.getIdCadastro(),
                tipos.length == 0 ? null : List.of(tipos), null, null, pagina()).getContent();
    }

    private PageRequest pagina() {
        return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dataHora"));
    }

    private Especialista especialista(String nome, String cpf) {
        Especialista especialista = new Especialista();
        preencherPessoa(especialista, nome, cpf);
        especialista.setEmail(cpf + "@email.com");
        especialista.setCargo(cargo);
        especialista.setRegistroConselho("CRP-" + cpf);
        especialista.setEspecialidades(new ArrayList<>());
        especialista.setJornadaTrabalho(new ArrayList<>());
        entityManager.persist(especialista);
        return especialista;
    }

    private Responsavel responsavel(String nome, String cpf) {
        Responsavel responsavel = new Responsavel();
        preencherPessoa(responsavel, nome, cpf);
        entityManager.persist(responsavel);
        return responsavel;
    }

    private Paciente paciente(String nome, String cpf, Responsavel responsavel) {
        Paciente paciente = new Paciente();
        preencherPessoa(paciente, nome, cpf);
        paciente.setCelular("15999990000");
        paciente.setStatusPaciente(StatusPaciente.MATRICULADO);
        paciente.setDataCadastro(LocalDate.of(2026, 1, 1));
        paciente.setResponsaveis(new ArrayList<>());
        if (responsavel != null) {
            VinculoResponsavel vinculo = new VinculoResponsavel();
            vinculo.setPaciente(paciente);
            vinculo.setResponsavel(responsavel);
            vinculo.setGrauParentesco(GrauParentesco.MAE);
            paciente.getResponsaveis().add(vinculo);
        }
        entityManager.persist(paciente);
        return paciente;
    }

    private void preencherPessoa(Pessoa pessoa, String nome, String cpf) {
        pessoa.setNome(nome);
        pessoa.setCpf(cpf);
        pessoa.setDataNascimento(LocalDate.of(1990, 1, 1));
        pessoa.setSexo('F');
        pessoa.setEstadoCivil(EstadoCivil.SOLTEIRO);
    }
}
