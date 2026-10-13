package com.fatec.gisa.services.terapia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.terapia.request.PresencaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.dtos.terapia.response.ConflitoHorarioDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaDoEspecialistaDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaEspecialistaDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaListaDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaPacienteDTO;
import com.fatec.gisa.dtos.terapia.response.VerificacaoConflitoDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.terapia.PresencaPaciente;
import com.fatec.gisa.entities.terapia.Terapia;
import com.fatec.gisa.enums.Modalidade;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.StatusPresenca;
import com.fatec.gisa.enums.StatusTerapia;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.terapia.OcupacaoHorario;
import com.fatec.gisa.repositories.terapia.TerapiaRepository;
import com.fatec.gisa.services.BuscaPorIds;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TerapiaService {

    private static final String TITULO_COLETIVA = "Terapia coletiva";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Long NENHUMA_TERAPIA = 0L;
    // O servidor (Render) roda em UTC; "hoje" para a presença é o dia em Sorocaba
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final TerapiaRepository terapiaRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialistaRepository especialistaRepository;

    @Transactional
    public Terapia cadastrar(TerapiaCadastroRequestDTO dto) {
        Participantes participantes = validarParticipantes(dto);

        // A trava precisa vir antes da verificação: um cadastro concorrente com alguém em
        // comum espera esta transação terminar e, ao verificar, já enxerga esta terapia.
        List<Long> idsEnvolvidos = new ArrayList<>(participantes.idsPacientes());
        idsEnvolvidos.addAll(participantes.idsEspecialistas());
        terapiaRepository.bloquearPessoas(idsEnvolvidos);

        List<ConflitoHorarioDTO> conflitos = buscarConflitos(participantes);
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException(mensagemDeConflito(conflitos));
        }

        Terapia terapia = new Terapia();
        terapia.setDataHoraInicio(participantes.inicio());
        terapia.setDataHoraFim(participantes.fim());
        terapia.setModalidade(participantes.pacientes().size() > 1 ? Modalidade.COLETIVA : Modalidade.INDIVIDUAL);
        terapia.setStatus(StatusTerapia.AGENDADA);
        terapia.setEspecialistas(new ArrayList<>(participantes.especialistas()));
        terapia.setPresencaPacientes(new ArrayList<>());
        Terapia terapiaSalva = terapiaRepository.save(terapia);

        for (Paciente paciente : participantes.pacientes()) {
            PresencaPaciente presenca = new PresencaPaciente();
            presenca.setTerapia(terapiaSalva);
            presenca.setPaciente(paciente);
            presenca.setPresenca(StatusPresenca.PENDENTE);
            terapiaSalva.getPresencaPacientes().add(presenca);
        }

        return terapiaRepository.save(terapiaSalva);
    }

    @Transactional
    public Terapia registrarPresenca(Long idTerapia, Long idPaciente, PresencaRequestDTO dto) {
        Terapia terapia = terapiaRepository.findById(idTerapia)
                .orElseThrow(() -> new ResourceNotFoundException("Terapia não encontrada com ID: " + idTerapia));
        PresencaPaciente presenca = terapia.getPresencaPacientes().stream()
                .filter(item -> item.getPaciente().getIdCadastro().equals(idPaciente))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "O paciente com ID " + idPaciente + " não faz parte desta terapia."));

        if (terapia.getStatus() == StatusTerapia.CANCELADA) {
            throw new IllegalArgumentException("Não é possível registrar presença em uma terapia cancelada.");
        }

        StatusPresenca nova = dto.presenca();
        // Falta justificada pode ser avisada com antecedência; presença e falta, só a partir do dia
        boolean exigeDiaDaTerapia = nova == StatusPresenca.PRESENTE || nova == StatusPresenca.FALTA_INJUSTIFICADA;
        if (exigeDiaDaTerapia && terapia.getDataHoraInicio().toLocalDate().isAfter(LocalDate.now(FUSO))) {
            throw new IllegalArgumentException(
                    "Presença e falta só podem ser registradas a partir do dia da terapia.");
        }

        String justificativa = null;
        if (nova == StatusPresenca.FALTA_JUSTIFICADA) {
            if (dto.justificativa() == null || dto.justificativa().isBlank()) {
                throw new IllegalArgumentException("Informe a justificativa da falta.");
            }
            justificativa = dto.justificativa().trim();
        }

        boolean desfazendoFaltaJustificada = presenca.getPresenca() == StatusPresenca.FALTA_JUSTIFICADA
                && nova != StatusPresenca.FALTA_JUSTIFICADA;
        if (desfazendoFaltaJustificada) {
            validarHorarioAindaLivre(terapia, idPaciente);
        }

        presenca.setPresenca(nova);
        presenca.setJustificativa(justificativa);
        return terapiaRepository.save(terapia);
    }

    // A falta justificada libera o horário do paciente (e, se todos justificaram, o dos especialistas).
    // Ao desfazê-la, o horário volta a ser ocupado; se outra terapia já o ocupou, o pedido é recusado
    // para não gerar agendamento duplo. Usa a mesma trava do cadastro contra pedidos simultâneos.
    private void validarHorarioAindaLivre(Terapia terapia, Long idPaciente) {
        boolean horarioDosEspecialistasEstavaLiberado = horarioLiberado(terapia);
        List<Long> idsEspecialistas = horarioDosEspecialistasEstavaLiberado
                ? terapia.getEspecialistas().stream().map(Especialista::getIdCadastro).toList()
                : List.of();

        List<Long> idsEnvolvidos = new ArrayList<>(idsEspecialistas);
        idsEnvolvidos.add(idPaciente);
        terapiaRepository.bloquearPessoas(idsEnvolvidos);

        List<ConflitoHorarioDTO> conflitos = buscarConflitos(List.of(idPaciente), idsEspecialistas,
                terapia.getDataHoraInicio(), terapia.getDataHoraFim(), terapia.getIdTerapia());
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException("Não é possível desfazer a falta justificada: o horário já foi "
                    + "ocupado por outra terapia. " + mensagemDeConflito(conflitos));
        }
    }

    // Todos os pacientes justificaram a falta: o horário dos especialistas fica livre
    private boolean horarioLiberado(Terapia terapia) {
        return terapia.getStatus() != StatusTerapia.CANCELADA
                && !terapia.getPresencaPacientes().isEmpty()
                && terapia.getPresencaPacientes().stream()
                        .allMatch(presenca -> presenca.getPresenca() == StatusPresenca.FALTA_JUSTIFICADA);
    }

    // Mesmas validações do cadastro, sem gravar: permite ao front avisar antes de salvar
    @Transactional(readOnly = true)
    public VerificacaoConflitoDTO verificarConflitos(TerapiaCadastroRequestDTO dto) {
        List<ConflitoHorarioDTO> conflitos = buscarConflitos(validarParticipantes(dto));
        return new VerificacaoConflitoDTO(!conflitos.isEmpty(), conflitos);
    }

    @Transactional(readOnly = true)
    public Page<TerapiaListaDTO> listarPaginado(List<StatusTerapia> status, Pageable pageable) {
        Page<Terapia> pagina = status == null || status.isEmpty()
                ? terapiaRepository.findAll(pageable)
                : terapiaRepository.findByStatusIn(status, pageable);
        return pagina.map(this::toListaDTO);
    }

    @Transactional(readOnly = true)
    public TerapiaListaDTO buscarDetalhes(Long idTerapia) {
        return terapiaRepository.findById(idTerapia)
                .map(this::toListaDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Terapia não encontrada com ID: " + idTerapia));
    }

    // cpf vem do token JWT do usuário logado
    @Transactional(readOnly = true)
    public Page<TerapiaDoEspecialistaDTO> listarDoEspecialista(
            String cpf, List<StatusTerapia> status, Pageable pageable) {
        Especialista especialista = especialistaRepository.findByCpf(cpf)
                .orElseThrow(() -> new IllegalArgumentException(
                        "O usuário logado não é um especialista."));
        Long idEspecialista = especialista.getIdCadastro();

        Page<Terapia> pagina = status == null || status.isEmpty()
                ? terapiaRepository.findByEspecialistas_IdCadastro(idEspecialista, pageable)
                : terapiaRepository.findByEspecialistas_IdCadastroAndStatusIn(idEspecialista, status, pageable);

        return pagina.map(terapia -> new TerapiaDoEspecialistaDTO(
                terapia.getIdTerapia(),
                titulo(terapia),
                terapia.getModalidade(),
                terapia.getDataHoraInicio().toLocalDate(),
                terapia.getDataHoraInicio().toLocalTime(),
                terapia.getDataHoraFim().toLocalTime(),
                terapia.getStatus(),
                horarioLiberado(terapia),
                pacientes(terapia),
                terapia.getEspecialistas().stream()
                        .filter(outro -> !outro.getIdCadastro().equals(idEspecialista))
                        .map(Especialista::getNome)
                        .sorted()
                        .toList()));
    }

    // IDs repetidos são unificados; IDs inexistentes ou de cadastros inativos são recusados
    private <T extends Pessoa> List<T> buscarAtivos(
            List<Long> ids, Function<Set<Long>, List<T>> buscar, String rotulo) {
        List<T> encontrados = BuscaPorIds.buscarTodos(ids, buscar, Pessoa::getIdCadastro, rotulo + " não encontrado(s)");

        List<String> inativos = encontrados.stream()
                .filter(pessoa -> pessoa.getStatusCadastro() == StatusCadastro.INATIVO)
                .map(Pessoa::getNome)
                .toList();
        if (!inativos.isEmpty()) {
            throw new IllegalArgumentException(rotulo + " inativo(s): " + String.join(", ", inativos));
        }

        return encontrados;
    }

    private Participantes validarParticipantes(TerapiaCadastroRequestDTO dto) {
        if (!dto.horaTermino().isAfter(dto.horaInicio())) {
            throw new IllegalArgumentException("A hora de término deve ser posterior à hora de início.");
        }

        List<Paciente> pacientes = buscarAtivos(
                dto.pacientesIds(), pacienteRepository::findAllById, "Paciente(s)");
        List<Especialista> especialistas = buscarAtivos(
                dto.especialistasIds(), especialistaRepository::findAllById, "Especialista(s)");

        return new Participantes(
                pacientes,
                especialistas,
                dto.data().atTime(dto.horaInicio()),
                dto.data().atTime(dto.horaTermino()));
    }

    // Terapia nova: ainda não existe, então não há o que ignorar
    private List<ConflitoHorarioDTO> buscarConflitos(Participantes participantes) {
        return buscarConflitos(participantes.idsPacientes(), participantes.idsEspecialistas(),
                participantes.inicio(), participantes.fim(), NENHUMA_TERAPIA);
    }

    // idTerapiaIgnorada: a terapia que está sendo revalidada não conflita consigo mesma
    private List<ConflitoHorarioDTO> buscarConflitos(List<Long> idsPacientes, List<Long> idsEspecialistas,
            LocalDateTime inicio, LocalDateTime fim, Long idTerapiaIgnorada) {
        List<ConflitoHorarioDTO> conflitos = new ArrayList<>();
        if (!idsEspecialistas.isEmpty()) {
            terapiaRepository.buscarOcupacoesDeEspecialistas(idsEspecialistas, inicio, fim,
                    StatusTerapia.CANCELADA, StatusPresenca.FALTA_JUSTIFICADA, idTerapiaIgnorada)
                    .forEach(ocupacao -> conflitos.add(toConflitoDTO("ESPECIALISTA", ocupacao)));
        }
        if (!idsPacientes.isEmpty()) {
            terapiaRepository.buscarOcupacoesDePacientes(idsPacientes, inicio, fim,
                    StatusTerapia.CANCELADA, StatusPresenca.FALTA_JUSTIFICADA, idTerapiaIgnorada)
                    .forEach(ocupacao -> conflitos.add(toConflitoDTO("PACIENTE", ocupacao)));
        }
        return conflitos;
    }

    private ConflitoHorarioDTO toConflitoDTO(String tipo, OcupacaoHorario ocupacao) {
        return new ConflitoHorarioDTO(
                tipo,
                ocupacao.idCadastro(),
                ocupacao.nome(),
                ocupacao.idTerapia(),
                ocupacao.inicio().toLocalDate(),
                ocupacao.inicio().toLocalTime(),
                ocupacao.fim().toLocalTime());
    }

    // Ex.: "Conflito de horário: Ana Souza (especialista) já tem terapia das 09:00 às 10:00."
    private String mensagemDeConflito(List<ConflitoHorarioDTO> conflitos) {
        return "Conflito de horário: " + conflitos.stream()
                .map(conflito -> "%s (%s) já tem terapia das %s às %s".formatted(
                        conflito.nome(),
                        conflito.tipo().toLowerCase(),
                        conflito.horaInicio().format(HORA),
                        conflito.horaTermino().format(HORA)))
                .collect(Collectors.joining("; ")) + ".";
    }

    private record Participantes(
            List<Paciente> pacientes,
            List<Especialista> especialistas,
            LocalDateTime inicio,
            LocalDateTime fim) {

        List<Long> idsPacientes() {
            return pacientes.stream().map(Paciente::getIdCadastro).toList();
        }

        List<Long> idsEspecialistas() {
            return especialistas.stream().map(Especialista::getIdCadastro).toList();
        }
    }

    private TerapiaListaDTO toListaDTO(Terapia terapia) {
        return new TerapiaListaDTO(
                terapia.getIdTerapia(),
                titulo(terapia),
                terapia.getModalidade(),
                terapia.getDataHoraInicio().toLocalDate(),
                terapia.getDataHoraInicio().toLocalTime(),
                terapia.getDataHoraFim().toLocalTime(),
                terapia.getStatus(),
                horarioLiberado(terapia),
                pacientes(terapia),
                terapia.getEspecialistas().stream()
                        .sorted(Comparator.comparing(Especialista::getNome))
                        .map(especialista -> new TerapiaEspecialistaDTO(
                                especialista.getIdCadastro(),
                                especialista.getNome(),
                                especialista.getEspecialidades().stream()
                                        .map(Especialidade::getNome)
                                        .sorted()
                                        .toList()))
                        .toList());
    }

    private String titulo(Terapia terapia) {
        if (terapia.getModalidade() == Modalidade.COLETIVA || terapia.getPresencaPacientes().isEmpty()) {
            return TITULO_COLETIVA;
        }
        return terapia.getPresencaPacientes().get(0).getPaciente().getNome();
    }

    private List<TerapiaPacienteDTO> pacientes(Terapia terapia) {
        return terapia.getPresencaPacientes().stream()
                .sorted(Comparator.comparing(presenca -> presenca.getPaciente().getNome()))
                .map(presenca -> new TerapiaPacienteDTO(
                        presenca.getPaciente().getIdCadastro(),
                        presenca.getPaciente().getNome(),
                        presenca.getPresenca(),
                        presenca.getJustificativa()))
                .toList();
    }

}
