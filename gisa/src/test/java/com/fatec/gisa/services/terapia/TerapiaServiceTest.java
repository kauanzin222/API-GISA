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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.config.RelogioInicioDoDiaConfig;
import com.fatec.gisa.dtos.terapia.FiltroTerapiaDTO;
import com.fatec.gisa.dtos.terapia.request.PresencaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.StatusTerapiaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaPatchRequestDTO;
import com.fatec.gisa.dtos.terapia.request.VerificacaoConflitoRequestDTO;
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
@Import(RelogioInicioDoDiaConfig.class)
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

    private static final LocalDate DATA = LocalDate.of(2099, 10, 27);
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

        var verificacao = terapiaService.verificarConflitos(verificacao(
                List.of(lucas.getIdCadastro(), maria.getIdCadastro()), List.of(carlos.getIdCadastro()),
                "09:30", "10:30"));

        assertThat(verificacao.possuiConflito()).isTrue();
        assertThat(verificacao.conflitos()).singleElement().satisfies(conflito -> {
            assertThat(conflito.tipo()).isEqualTo("PACIENTE");
            assertThat(conflito.nome()).isEqualTo("Lucas Martins");
            assertThat(conflito.idTerapia()).isEqualTo(existente.getIdTerapia());
            assertThat(conflito.horaInicio()).isEqualTo(LocalTime.of(9, 0));
        });
        assertThat(terapiaService.listarPaginado(FiltroTerapiaDTO.semFiltro(), PageRequest.of(0, 6)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void verificacaoSemConflito() {
        var verificacao = terapiaService.verificarConflitos(verificacao(
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

        var pagina = terapiaService.listarDoEspecialista(ana.getCpf(), FiltroTerapiaDTO.semFiltro(), PageRequest.of(0, 3));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        TerapiaDoEspecialistaDTO terapia = pagina.getContent().get(0);
        assertThat(terapia.outrosEspecialistas()).containsExactly("Carlos Lima");
        assertThat(terapia.pacientes()).hasSize(2);
    }

    @Test
    void filtraListagemPorStatus() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00"));

        assertThat(terapiaService.listarPaginado(porStatus(StatusTerapia.AGENDADA), PageRequest.of(0, 6))
                .getTotalElements()).isEqualTo(1);
        assertThat(terapiaService.listarPaginado(porStatus(StatusTerapia.CANCELADA), PageRequest.of(0, 6))
                .getTotalElements()).isZero();
    }

    @Test
    void recusaUsuarioQueNaoEEspecialista() {
        assertThatThrownBy(() -> terapiaService.listarDoEspecialista(
                lucas.getCpf(), FiltroTerapiaDTO.semFiltro(), PageRequest.of(0, 3)))
                .hasMessage("O usuário logado não é um especialista.");
    }

    // ---------- Filtros da listagem ----------

    @Test
    void filtraPorDiaEPorIntervaloComOsDoisDiasIncluidos() {
        terapiaEm(DATA, lucas, ana, "09:00", "10:00");
        terapiaEm(DATA.plusDays(1), lucas, ana, "23:00", "23:50");
        terapiaEm(DATA.plusDays(2), lucas, ana, "00:00", "00:50");

        assertThat(total(filtro(null, DATA, null, null, null, null))).isEqualTo(1);
        assertThat(total(filtro(null, null, DATA, DATA.plusDays(1), null, null))).isEqualTo(2);
        assertThat(total(filtro(null, null, DATA.plusDays(1), null, null, null))).isEqualTo(2);
        assertThat(total(filtro(null, null, null, DATA, null, null))).isEqualTo(1);
        assertThat(total(filtro(null, null, DATA, DATA, null, null))).isEqualTo(1);
        assertThat(total(filtro(null, DATA.minusDays(1), null, null, null, null))).isZero();
    }

    @Test
    void filtraPorPacienteEEspecialistaSemRepetirTerapiaColetiva() {
        terapiaService.cadastrar(requisicao(
                List.of(lucas.getIdCadastro(), maria.getIdCadastro()),
                List.of(ana.getIdCadastro(), carlos.getIdCadastro()), "09:00", "10:00"));
        terapiaService.cadastrar(requisicao(
                List.of(maria.getIdCadastro()), List.of(carlos.getIdCadastro()), "14:00", "15:00"));

        assertThat(total(filtro(null, null, null, null, lucas.getIdCadastro(), null))).isEqualTo(1);
        assertThat(total(filtro(null, null, null, null, maria.getIdCadastro(), null))).isEqualTo(2);
        assertThat(total(filtro(null, null, null, null, null, ana.getIdCadastro()))).isEqualTo(1);
        assertThat(total(filtro(null, null, null, null, null, carlos.getIdCadastro()))).isEqualTo(2);
        // Combinados: Maria com Ana só na coletiva
        assertThat(total(filtro(null, DATA, null, null, maria.getIdCadastro(), ana.getIdCadastro()))).isEqualTo(1);
        // Filtro de ID que não participa de nada: página vazia, não erro
        assertThat(total(filtro(null, null, null, null, 9999L, null))).isZero();
    }

    @Test
    void combinaFiltrosComStatusEOrdenaPeloInicio() {
        Long tarde = terapiaEm(DATA, lucas, ana, "14:00", "15:00");
        Long manha = terapiaEm(DATA, maria, ana, "08:00", "09:00");
        entityManager.find(Terapia.class, tarde).setStatus(StatusTerapia.CANCELADA);

        var pagina = terapiaService.listarPaginado(filtro(List.of(StatusTerapia.AGENDADA), DATA, null, null,
                null, ana.getIdCadastro()), PageRequest.of(0, 100));
        assertThat(pagina.getContent()).extracting(TerapiaListaDTO::idTerapia).containsExactly(manha);

        var crescente = terapiaService.listarPaginado(filtro(null, DATA, null, null, null, null),
                PageRequest.of(0, 100, org.springframework.data.domain.Sort.by("dataHoraInicio").ascending()));
        assertThat(crescente.getContent()).extracting(TerapiaListaDTO::idTerapia).containsExactly(manha, tarde);
    }

    @Test
    void minhasTerapiasAceitamFiltroPorDia() {
        terapiaEm(DATA, lucas, ana, "09:00", "10:00");
        terapiaEm(DATA.plusDays(1), lucas, ana, "09:00", "10:00");

        assertThat(terapiaService.listarDoEspecialista(ana.getCpf(), filtro(null, DATA, null, null, null, null),
                PageRequest.of(0, 3)).getTotalElements()).isEqualTo(1);
        assertThat(terapiaService.listarDoEspecialista(ana.getCpf(),
                filtro(null, null, DATA, DATA.plusDays(1), null, null), PageRequest.of(0, 3))
                .getTotalElements()).isEqualTo(2);
    }

    @Test
    void recusaFiltrosInvalidos() {
        assertThatThrownBy(() -> filtro(null, DATA, DATA, null, null, null))
                .hasMessage("Use 'data' ou o intervalo 'inicio'/'fim', não os dois juntos.");
        assertThatThrownBy(() -> filtro(null, DATA, null, DATA, null, null))
                .hasMessage("Use 'data' ou o intervalo 'inicio'/'fim', não os dois juntos.");
        assertThatThrownBy(() -> filtro(null, null, DATA, DATA.minusDays(1), null, null))
                .hasMessage("A data final deve ser igual ou posterior à data inicial.");
        assertThatThrownBy(() -> filtro(null, null, null, null, 0L, null))
                .hasMessage("Valor inválido para 'idPaciente': 0");
        assertThatThrownBy(() -> filtro(null, null, null, null, null, -3L))
                .hasMessage("Valor inválido para 'idEspecialista': -3");
        assertThatThrownBy(() -> terapiaService.listarPaginado(FiltroTerapiaDTO.semFiltro(), PageRequest.of(0, 101)))
                .hasMessage("O parâmetro 'size' deve ser no máximo 100.");
        assertThatThrownBy(() -> terapiaService.listarDoEspecialista(ana.getCpf(), FiltroTerapiaDTO.semFiltro(),
                PageRequest.of(0, 101)))
                .hasMessage("O parâmetro 'size' deve ser no máximo 100.");
    }

    private long total(FiltroTerapiaDTO filtro) {
        return terapiaService.listarPaginado(filtro, PageRequest.of(0, 100)).getTotalElements();
    }

    private FiltroTerapiaDTO filtro(List<StatusTerapia> status, LocalDate data, LocalDate inicio, LocalDate fim,
            Long idPaciente, Long idEspecialista) {
        return FiltroTerapiaDTO.de(status, data, inicio, fim, idPaciente, idEspecialista);
    }

    private FiltroTerapiaDTO porStatus(StatusTerapia status) {
        return filtro(List.of(status), null, null, null, null, null);
    }

    // ---------- Status ----------

    @Test
    void percorreOFluxoAteConcluirEOStatusFinalNaoMudaMais() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");

        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.EM_ATENDIMENTO, "ignorado"));
        assertThat(terapiaService.buscarDetalhes(idTerapia).status()).isEqualTo(StatusTerapia.EM_ATENDIMENTO);
        assertThat(terapiaService.buscarDetalhes(idTerapia).motivoCancelamento()).isNull();

        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.CONCLUIDA, null));
        assertThat(terapiaService.buscarDetalhes(idTerapia).status()).isEqualTo(StatusTerapia.CONCLUIDA);

        assertThatThrownBy(() -> terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.CANCELADA, "Tarde demais")))
                .hasMessage("A terapia está concluída e não pode mais mudar de status.");
    }

    @Test
    void cancelaComMotivoELiberaOHorario() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");

        terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.CANCELADA, "  Especialista em licença  "));

        TerapiaListaDTO detalhes = terapiaService.buscarDetalhes(idTerapia);
        assertThat(detalhes.status()).isEqualTo(StatusTerapia.CANCELADA);
        assertThat(detalhes.motivoCancelamento()).isEqualTo("Especialista em licença");
        assertThat(terapiaService.verificarConflitos(verificacao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:00", "10:00")).possuiConflito())
                .isFalse();
        assertThat(terapiaEm(DATA, lucas, ana, "09:00", "10:00")).isNotNull();

        assertThatThrownBy(() -> terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.AGENDADA, null)))
                .hasMessage("A terapia está cancelada e não pode mais mudar de status.");
    }

    @Test
    void cancelamentoEmAtendimentoTambemEPermitido() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");
        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.EM_ATENDIMENTO, null));

        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.CANCELADA, "Queda de energia"));

        assertThat(terapiaService.buscarDetalhes(idTerapia).status()).isEqualTo(StatusTerapia.CANCELADA);
    }

    @Test
    void recusaTransicoesInvalidasECancelamentoSemMotivo() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");

        assertThatThrownBy(() -> terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.CANCELADA, "   ")))
                .hasMessage("Informe o motivo do cancelamento.");
        assertThatThrownBy(() -> terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.CONCLUIDA, null)))
                .hasMessage("Não é possível alterar o status de AGENDADA para CONCLUIDA. "
                        + "A partir de AGENDADA, os status permitidos são: EM_ATENDIMENTO, CANCELADA.");
        assertThatThrownBy(() -> terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.AGENDADA, null)))
                .hasMessage("A terapia já está com o status AGENDADA.");

        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.EM_ATENDIMENTO, null));
        assertThatThrownBy(() -> terapiaService.alterarStatus(idTerapia,
                new StatusTerapiaRequestDTO(StatusTerapia.AGENDADA, null)))
                .hasMessage("Não é possível alterar o status de EM_ATENDIMENTO para AGENDADA. "
                        + "A partir de EM_ATENDIMENTO, os status permitidos são: CONCLUIDA, CANCELADA.");
        assertThat(terapiaService.buscarDetalhes(idTerapia).status()).isEqualTo(StatusTerapia.EM_ATENDIMENTO);
    }

    @Test
    void alterarStatusDeTerapiaInexistenteRetorna404() {
        assertThatThrownBy(() -> terapiaService.alterarStatus(9999L,
                new StatusTerapiaRequestDTO(StatusTerapia.EM_ATENDIMENTO, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Terapia não encontrada com ID: 9999");
    }

    // ---------- Horário no passado ----------

    // O relógio dos testes está parado na meia-noite de hoje
    @Test
    void naoAgendaTerapiaEmHorarioQueJaPassou() {
        assertThatThrownBy(() -> terapiaEm(HOJE.minusDays(1), lucas, ana, "09:00", "10:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Não é possível agendar uma terapia para um horário que já passou.");
        assertThatThrownBy(() -> terapiaHoje(lucas, ana, "00:00", "00:50"))
                .hasMessage("Não é possível agendar uma terapia para um horário que já passou.");

        assertThat(terapiaHoje(lucas, ana, "00:01", "00:50")).isNotNull();
    }

    @Test
    void naoRemarcaTerapiaParaHorarioQueJaPassou() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setData(HOJE.minusDays(1));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, dto))
                .hasMessage("Não é possível agendar uma terapia para um horário que já passou.");
    }

    // Terapia agendada cujo horário já chegou: trocar participantes não é remarcar
    @Test
    void trocaParticipantesDeTerapiaJaIniciadaSemRemarcar() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        Terapia terapia = entityManager.find(Terapia.class, idTerapia);
        terapia.setDataHoraInicio(HOJE.minusDays(1).atTime(9, 0));
        terapia.setDataHoraFim(HOJE.minusDays(1).atTime(10, 0));
        entityManager.flush();

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setEspecialistasIds(List.of(carlos.getIdCadastro()));
        terapiaService.atualizar(idTerapia, dto);

        assertThat(terapiaService.buscarDetalhes(idTerapia).especialistas())
                .extracting("nome").containsExactly("Carlos Lima");
    }

    // ---------- Edição ----------

    @Test
    void remarcaMantendoPresencaEJustificativaDeQuemContinua() {
        Long idTerapia = terapiaHoje(List.of(lucas, maria), ana, "09:00", "10:00");
        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Consulta médica"));

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setData(HOJE.plusDays(1));
        dto.setHoraInicio(LocalTime.of(11, 0));
        dto.setHoraTermino(LocalTime.of(12, 0));
        terapiaService.atualizar(idTerapia, dto);
        entityManager.flush();
        entityManager.clear();

        TerapiaListaDTO detalhes = terapiaService.buscarDetalhes(idTerapia);
        assertThat(detalhes.data()).isEqualTo(HOJE.plusDays(1));
        assertThat(detalhes.horaInicio()).isEqualTo(LocalTime.of(11, 0));
        assertThat(detalhes.horaTermino()).isEqualTo(LocalTime.of(12, 0));
        assertThat(detalhes.pacientes()).extracting("presenca")
                .containsExactly(StatusPresenca.FALTA_JUSTIFICADA, StatusPresenca.PENDENTE);
        assertThat(detalhes.pacientes().get(0).justificativa()).isEqualTo("Consulta médica");
        assertThat(detalhes.especialistas()).extracting("nome").containsExactly("Ana Souza");
    }

    @Test
    void trocaParticipantesERecalculaAModalidade() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");
        Paciente bia = paciente("Beatriz Lima", "20000000003");

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setPacientesIds(List.of(lucas.getIdCadastro(), bia.getIdCadastro(), bia.getIdCadastro()));
        dto.setEspecialistasIds(List.of(carlos.getIdCadastro()));
        terapiaService.atualizar(idTerapia, dto);

        TerapiaListaDTO coletiva = terapiaService.buscarDetalhes(idTerapia);
        assertThat(coletiva.modalidade()).isEqualTo(Modalidade.COLETIVA);
        assertThat(coletiva.titulo()).isEqualTo("Terapia coletiva");
        assertThat(coletiva.pacientes()).extracting("nome").containsExactly("Beatriz Lima", "Lucas Martins");
        assertThat(coletiva.especialistas()).extracting("nome").containsExactly("Carlos Lima");
        assertThat(coletiva.horaInicio()).isEqualTo(LocalTime.of(9, 0));

        TerapiaPatchRequestDTO volta = new TerapiaPatchRequestDTO();
        volta.setPacientesIds(List.of(bia.getIdCadastro()));
        terapiaService.atualizar(idTerapia, volta);
        entityManager.flush();
        entityManager.clear();

        TerapiaListaDTO individual = terapiaService.buscarDetalhes(idTerapia);
        assertThat(individual.modalidade()).isEqualTo(Modalidade.INDIVIDUAL);
        assertThat(individual.titulo()).isEqualTo("Beatriz Lima");
        // Ana saiu da terapia: o horário dela ficou livre
        assertThat(terapiaEm(DATA, maria, ana, "09:00", "10:00")).isNotNull();
    }

    @Test
    void edicaoIgnoraAPropriaTerapiaMasRecusaConflitoComOutras() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");
        terapiaEm(DATA, maria, ana, "11:00", "12:00");

        TerapiaPatchRequestDTO sobreposta = new TerapiaPatchRequestDTO();
        sobreposta.setHoraInicio(LocalTime.of(9, 30));
        sobreposta.setHoraTermino(LocalTime.of(10, 30));
        terapiaService.atualizar(idTerapia, sobreposta);
        assertThat(terapiaService.buscarDetalhes(idTerapia).horaInicio()).isEqualTo(LocalTime.of(9, 30));

        TerapiaPatchRequestDTO conflitante = new TerapiaPatchRequestDTO();
        conflitante.setHoraInicio(LocalTime.of(11, 30));
        conflitante.setHoraTermino(LocalTime.of(12, 30));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, conflitante))
                .hasMessage("Conflito de horário: Ana Souza (especialista) já tem terapia das 11:00 às 12:00.");
    }

    @Test
    void verificacaoComTerapiaIgnoradaValidaAEdicaoAntesDeSalvar() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");

        assertThat(terapiaService.verificarConflitos(verificacao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:30", "10:30")).possuiConflito())
                .isTrue();
        assertThat(terapiaService.verificarConflitos(verificacao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:30", "10:30", idTerapia))
                .possuiConflito())
                .isFalse();
        assertThatThrownBy(() -> terapiaService.verificarConflitos(verificacao(
                List.of(lucas.getIdCadastro()), List.of(ana.getIdCadastro()), "09:30", "10:30", 9999L)))
                .hasMessage("Terapia não encontrada com ID: 9999");
    }

    @Test
    void soEditaTerapiaAgendada() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");
        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.EM_ATENDIMENTO, null));

        TerapiaPatchRequestDTO dto = new TerapiaPatchRequestDTO();
        dto.setHoraInicio(LocalTime.of(8, 0));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, dto))
                .hasMessage("Só é possível editar terapias agendadas. Esta terapia está em atendimento.");

        terapiaService.alterarStatus(idTerapia, new StatusTerapiaRequestDTO(StatusTerapia.CANCELADA, "Teste"));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, dto))
                .hasMessage("Só é possível editar terapias agendadas. Esta terapia está cancelada.");
    }

    @Test
    void edicaoRevalidaCamposHorarioIdsECadastrosAtivos() {
        Long idTerapia = terapiaEm(DATA, lucas, ana, "09:00", "10:00");

        TerapiaPatchRequestDTO dataNula = new TerapiaPatchRequestDTO();
        dataNula.setData(null);
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, dataNula))
                .hasMessage("A data não pode ser nula.");

        TerapiaPatchRequestDTO semPacientes = new TerapiaPatchRequestDTO();
        semPacientes.setPacientesIds(List.of());
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, semPacientes))
                .hasMessage("A terapia deve ter ao menos um paciente.");

        TerapiaPatchRequestDTO semEspecialistas = new TerapiaPatchRequestDTO();
        semEspecialistas.setEspecialistasIds(null);
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, semEspecialistas))
                .hasMessage("A terapia deve ter ao menos um especialista.");

        // Só o término enviado: comparado com o início atual (09:00)
        TerapiaPatchRequestDTO terminoAntes = new TerapiaPatchRequestDTO();
        terminoAntes.setHoraTermino(LocalTime.of(8, 30));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, terminoAntes))
                .hasMessage("A hora de término deve ser posterior à hora de início.");

        TerapiaPatchRequestDTO inexistente = new TerapiaPatchRequestDTO();
        inexistente.setPacientesIds(List.of(lucas.getIdCadastro(), 9999L));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, inexistente))
                .hasMessage("Paciente(s) não encontrado(s): 9999");

        maria.setStatusCadastro(com.fatec.gisa.enums.StatusCadastro.INATIVO);
        TerapiaPatchRequestDTO inativo = new TerapiaPatchRequestDTO();
        inativo.setPacientesIds(List.of(lucas.getIdCadastro(), maria.getIdCadastro()));
        assertThatThrownBy(() -> terapiaService.atualizar(idTerapia, inativo))
                .hasMessage("Paciente(s) inativo(s): Maria Costa");

        assertThatThrownBy(() -> terapiaService.atualizar(9999L, new TerapiaPatchRequestDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Terapia não encontrada com ID: 9999");
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
    void antesDoDiaSoPermiteDesfazerEFaltaJustificada() {
        Long idTerapia = terapiaEm(HOJE.plusDays(7), lucas, ana, "09:00", "10:00");

        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).presencasPermitidas())
                .containsExactly(StatusPresenca.PENDENTE, StatusPresenca.FALTA_JUSTIFICADA);
    }

    @Test
    void aceitaPresencaAteOFimDoDiaSeguinte() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        moverDias(idTerapia, -1);

        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).presencasPermitidas())
                .containsExactly(StatusPresenca.values());
        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.PRESENTE, null));
        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).presenca())
                .isEqualTo(StatusPresenca.PRESENTE);
    }

    @Test
    void depoisDoPrazoSoAceitaFaltaJustificada() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        moverDias(idTerapia, -2);

        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).presencasPermitidas())
                .containsExactly(StatusPresenca.FALTA_JUSTIFICADA);
        for (StatusPresenca presenca : List.of(StatusPresenca.PRESENTE, StatusPresenca.FALTA_INJUSTIFICADA,
                StatusPresenca.PENDENTE)) {
            assertThatThrownBy(() -> terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                    new PresencaRequestDTO(presenca, null)))
                    .hasMessage("O prazo para registrar presença ou falta nesta terapia terminou em "
                            + HOJE.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + ". Agora só é possível registrar falta justificada.");
        }

        terapiaService.registrarPresenca(idTerapia, lucas.getIdCadastro(),
                new PresencaRequestDTO(StatusPresenca.FALTA_JUSTIFICADA, "Atestado entregue depois"));
        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).presenca())
                .isEqualTo(StatusPresenca.FALTA_JUSTIFICADA);
    }

    @Test
    void terapiaCanceladaNaoPermiteNenhumaPresenca() {
        Long idTerapia = terapiaHoje(lucas, ana, "09:00", "10:00");
        entityManager.find(Terapia.class, idTerapia).setStatus(StatusTerapia.CANCELADA);

        assertThat(terapiaService.buscarDetalhes(idTerapia).pacientes().get(0).presencasPermitidas()).isEmpty();
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

    // Não dá para agendar no passado: a terapia é criada hoje e a data é movida direto na entidade
    private void moverDias(Long idTerapia, int dias) {
        Terapia terapia = entityManager.find(Terapia.class, idTerapia);
        terapia.setDataHoraInicio(terapia.getDataHoraInicio().plusDays(dias));
        terapia.setDataHoraFim(terapia.getDataHoraFim().plusDays(dias));
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

    private VerificacaoConflitoRequestDTO verificacao(List<Long> pacientes, List<Long> especialistas,
            String inicio, String termino) {
        return verificacao(pacientes, especialistas, inicio, termino, null);
    }

    private VerificacaoConflitoRequestDTO verificacao(List<Long> pacientes, List<Long> especialistas,
            String inicio, String termino, Long idTerapiaIgnorada) {
        return new VerificacaoConflitoRequestDTO(pacientes, especialistas, DATA,
                LocalTime.parse(inicio), LocalTime.parse(termino), idTerapiaIgnorada);
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
