package com.fatec.gisa.services.terapia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.terapia.request.PresencaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaDoEspecialistaDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaListaDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.entities.profissional.Cbo;
import com.fatec.gisa.entities.terapia.Terapia;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.Modalidade;
import com.fatec.gisa.enums.StatusPaciente;
import com.fatec.gisa.enums.StatusPresenca;
import com.fatec.gisa.enums.StatusTerapia;
import com.fatec.gisa.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityManager;

// Banco H2 em memória, sem as migrations (que usam sintaxe do PostgreSQL)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:terapia;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Transactional
class TerapiaServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 27);
    private static final LocalDate HOJE = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));

    @Autowired
    private TerapiaService terapiaService;

    @Autowired
    private EntityManager entityManager;

    private Cargo cargo;
    private Especialista ana;
    private Especialista carlos;
    private Paciente lucas;
    private Paciente maria;

    @BeforeEach
    void preparar() {
        Cbo cbo = new Cbo("999999", "Teste");
        entityManager.persist(cbo);
        cargo = new Cargo(null, "Terapeuta", cbo);
        entityManager.persist(cargo);

        ana = especialista("Ana Souza", "10000000001");
        carlos = especialista("Carlos Lima", "10000000002");
        lucas = paciente("Lucas Martins", "20000000001");
        maria = paciente("Maria Costa", "20000000002");
        entityManager.flush();
    }

    @Test
    void cadastraTerapiaColetivaQuandoHaMaisDeUmPaciente() {
        var terapia = terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro(), maria.getIdCadastro(), maria.getIdCadastro()),
                List.of(ana.getIdCadastro(), carlos.getIdCadastro()),
                "09:00", "10:00"));
        entityManager.flush();
        entityManager.clear();

        TerapiaListaDTO detalhes = terapiaService.buscarDetalhes(terapia.getIdTerapia());

        assertThat(detalhes.modalidade()).isEqualTo(Modalidade.COLETIVA);
        assertThat(detalhes.titulo()).isEqualTo("Terapia coletiva");
        assertThat(detalhes.status()).isEqualTo(StatusTerapia.AGENDADA);
        assertThat(detalhes.data()).isEqualTo(DATA);
        assertThat(detalhes.pacientes()).extracting("nome").containsExactly("Lucas Martins", "Maria Costa");
        assertThat(detalhes.pacientes()).extracting("presenca").containsOnly(StatusPresenca.PENDENTE);
        assertThat(detalhes.especialistas()).extracting("nome").containsExactly("Ana Souza", "Carlos Lima");
    }

    @Test
    void terapiaIndividualUsaNomeDoPacienteComoTitulo() {
        var terapia = terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "08:00", "08:50"));

        TerapiaListaDTO detalhes = terapiaService.buscarDetalhes(terapia.getIdTerapia());

        assertThat(detalhes.modalidade()).isEqualTo(Modalidade.INDIVIDUAL);
        assertThat(detalhes.titulo()).isEqualTo("Lucas Martins");
    }

    @Test
    void recusaHoraDeTerminoAntesDoInicio() {
        assertThatThrownBy(() -> terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "10:00", "09:00")))
                .hasMessage("A hora de término deve ser posterior à hora de início.");
    }

    @Test
    void recusaEspecialistaComOutraTerapiaNoMesmoHorario() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        assertThatThrownBy(() -> terapiaService.cadastrar(requisicao(
                List.of(maria.getIdCadastro()), List.of(ana.getIdCadastro()), "09:30", "10:30")))
                .hasMessage("Conflito de horário: Ana Souza (especialista) já tem terapia das 09:00 às 10:00.");
    }

    @Test
    void mensagemListaTodosOsConflitos() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        assertThatThrownBy(() -> terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:30", "10:30")))
                .hasMessage("Conflito de horário: Ana Souza (especialista) já tem terapia das 09:00 às 10:00; "
                        + "Lucas Martins (paciente) já tem terapia das 09:00 às 10:00.");
    }

    @Test
    void verificacaoApontaConflitosSemGravar() {
        var existente = terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        var verificacao = terapiaService.verificarConflitos(requisicao(
                List.of(lucas.getIdCadastro(), maria.getIdCadastro()), List.of(carlos.getIdCadastro()),
                "09:30", "10:30"));

        assertThat(verificacao.possuiConflito()).isTrue();
        assertThat(verificacao.conflitos()).singleElement().satisfies(conflito -> {
            assertThat(conflito.tipo()).isEqualTo("PACIENTE");
            assertThat(conflito.nome()).isEqualTo("Lucas Martins");
            assertThat(conflito.idTerapia()).isEqualTo(existente.getIdTerapia());
            assertThat(conflito.horaInicio()).isEqualTo(LocalTime.of(9, 0));
        });
        assertThat(terapiaService.listarPaginado(null, PageRequest.of(0, 6)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void verificacaoSemConflito() {
        var verificacao = terapiaService.verificarConflitos(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        assertThat(verificacao.possuiConflito()).isFalse();
        assertThat(verificacao.conflitos()).isEmpty();
    }

    @Test
    void recusaPacienteComOutraTerapiaNoMesmoHorario() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        assertThatThrownBy(() -> terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(carlos.getIdCadastro()), "09:59", "11:00")))
                .hasMessage("Conflito de horário: Lucas Martins (paciente) já tem terapia das 09:00 às 10:00.");
    }

    @Test
    void permiteTerapiaQueComecaQuandoAOutraTermina() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        var seguinte = terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "10:00", "11:00"));

        assertThat(seguinte.getIdTerapia()).isNotNull();
    }

    @Test
    void recusaIdsInexistentes() {
        assertThatThrownBy(() -> terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro(), 9999L), List.of(ana.getIdCadastro()), "09:00", "10:00")))
                .hasMessage("Paciente(s) não encontrado(s): 9999");
    }

    @Test
    void listaSomenteAsTerapiasDoEspecialistaLogado() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro(), maria.getIdCadastro()),
                List.of(ana.getIdCadastro(), carlos.getIdCadastro()), "09:00", "10:00"));
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(carlos.getIdCadastro()), "14:00", "15:00"));
        entityManager.flush();
        entityManager.clear();

        var pagina = terapiaService.listarDoEspecialista(ana.getCpf(), null, PageRequest.of(0, 3));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        TerapiaDoEspecialistaDTO terapia = pagina.getContent().get(0);
        assertThat(terapia.outrosEspecialistas()).containsExactly("Carlos Lima");
        assertThat(terapia.pacientes()).hasSize(2);
    }

    @Test
    void filtraListagemPorStatus() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        assertThat(terapiaService.listarPaginado(List.of(StatusTerapia.AGENDADA), PageRequest.of(0, 6))
                .getTotalElements()).isEqualTo(1);
        assertThat(terapiaService.listarPaginado(List.of(StatusTerapia.CANCELADA), PageRequest.of(0, 6))
                .getTotalElements()).isZero();
    }

    @Test
    void recusaUsuarioQueNaoEEspecialista() {
        assertThatThrownBy(() -> terapiaService.listarDoEspecialista(
                lucas.getCpf(), null, PageRequest.of(0, 3)))
                .hasMessage("O usuário logado não é um especialista.");
    }

    // ---------- Presença ----------

    @Test
    void registraPresencaNoDiaDaTerapia() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.PRESENTE, "texto ignorado"));

        var paciente = terapiaService.buscarDetalhes(idTerapia).pacientes().get(0);
        assertThat(paciente.presenca()).isEqualTo(StatusPresenca.PRESENTE);
        assertThat(paciente.justificativa()).isNull();
    }

    @Test
    void recusaPresencaEFaltaAntesDoDiaDaTerapia() {
        Long idTerapia = terapiaEm(HOJE.plusDays(7), lucas, ana, "09:00", "10:00");

        for (StatusPresenca presenca : List.of(StatusPresenca.PRESENTE, StatusPresenca.FALTA_INJUSTIFICADA)) {
            assertThatThrownBy(() -> terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                    new PresencaRequestDTO(presenca, null)))
                    .hasMessage("Presença e falta só podem ser registradas a partir do dia da terapia.");
        }
    }

    @Test
    void faltaJustificadaPodeSerAvisadaAntesEExigeJustificativa() {
        Long idTerapia = terapiaEm(HOJE.plusDays(7), lucas, ana, "09:00", "10:00");

        assertThatThrownBy(() -> terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "  ")))
                .hasMessage("Informe a justificativa da falta.");

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "  Atestado médico  "));
        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).justificativa())
                .isEqualTo("Atestado médico");
    }

    @Test
    void recusaPacienteQueNaoFazParteDaTerapia() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");

        assertThatThrownBy(() -> terapiaService.registrarPresenca(idTerapia, maria.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.PRESENTE, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recusaPresencaEmTerapiaCancelada() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        entityManager.find(Terapia.class, idTerapia).setStatus(StatusTerapia.CANCELADA);

        assertThatThrownBy(() -> terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.PRESENTE, null)))
                .hasMessage("Não é possível registrar presença em uma terapia cancelada.");
    }

    @Test
    void faltaJustificadaDeTodosLiberaOHorario() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Doente"));

        assertThat(terapiaService.buscarDetalhes(idTerapia).horarioLiberado()).isTrue();
        // O especialista recebe outro paciente no horário liberado...
        assertThat(terapiaHoje(maria, ana, "09:00", "10:00")).isNotNull();
        // ...e o paciente que justificou a falta também fica livre naquele horário
        assertThat(terapiaHoje(lucas, carlos, "09:00", "10:00")).isNotNull();
    }

    @Test
    void faltaJustificadaDeUmSoPacienteNaoLiberaOHorarioDoEspecialista() {
        Long idTerapia = terapiaHoje(List.of(lucas, maria), ana, "09:00", "10:00");

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Doente"));

        assertThat(terapiaService.buscarDetalhes(idTerapia).horarioLiberado()).isFalse();
        assertThatThrownBy(() -> terapiaHoje(List.of(paciente("Outro", "20000000009")), ana, "09:00", "10:00"))
                .hasMessageStartingWith("Conflito de horário: Ana Souza (especialista)");
    }

    @Test
    void naoDesfazFaltaJustificadaSeOHorarioJaFoiOcupado() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Doente"));
        terapiaHoje(maria, ana, "09:00", "10:00");

        assertThatThrownBy(() -> terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.PENDENTE, null)))
                .hasMessageStartingWith("Não é possível desfazer a falta justificada: o horário já foi ocupado")
                .hasMessageContaining("Ana Souza (especialista)");
    }

    @Test
    void desfazFaltaJustificadaQuandoOHorarioContinuaLivre() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Doente"));

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.PENDENTE, null));

        var detalhes = terapiaService.buscarDetalhes(idTerapia);
        assertThat(detalhes.horarioLiberado()).isFalse();
        assertThat(detalhes.pacientes().get(0).presenca()).isEqualTo(StatusPresenca.PENDENTE);
        assertThat(detalhes.pacientes().get(0).justificativa()).isNull();
    }

    private Long terapiaHoje(Paciente paciente, Especialista especialista, String inicio, String termino) {
        return terapiaEm(HOJE, paciente, especialista, inicio, termino);
    }

    private Long terapiaHoje(List<Paciente> pacientes, Especialista especialista, String inicio, String termino) {
        return terapiaService.cadastrar(new TerapiaCadastroRequestDTO(
                pacientes.stream().map(Paciente::getIdCadastro).toList(), List.of(especialista.getIdCadastro()),
                HOJE, LocalTime.parse(inicio), LocalTime.parse(termino))).getIdTerapia();
    }

    private Long terapiaEm(LocalDate data, Paciente paciente, Especialista especialista,
            String inicio, String termino) {
        return terapiaService.cadastrar(new TerapiaCadastroRequestDTO(
                List.of(paciente.getIdCadastro()), List.of(especialista.getIdCadastro()),
                data, LocalTime.parse(inicio), LocalTime.parse(termino))).getIdTerapia();
    }

    private TerapiaCadastroRequestDTO requisicao(List<Long> pacientes, List<Long> especialistas,
            String inicio, String termino) {
        return new TerapiaCadastroRequestDTO(
                pacientes, especialistas, DATA, LocalTime.parse(inicio), LocalTime.parse(termino));
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

    private Paciente paciente(String nome, String cpf) {
        Paciente paciente = new Paciente();
        preencherPessoa(paciente, nome, cpf);
        paciente.setStatusPaciente(StatusPaciente.MATRICULADO);
        paciente.setDataCadastro(LocalDate.of(2026, 1, 1));
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
