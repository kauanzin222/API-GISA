package com.fatec.gisa.services.terapia;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TerapiaService {

    private static final String TITULO_COLETIVA = "Terapia coletiva";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

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
        Set<Long> idsUnicos = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<T> encontrados = buscar.apply(idsUnicos);

        Set<Long> naoEncontrados = new LinkedHashSet<>(idsUnicos);
        encontrados.forEach(pessoa -> naoEncontrados.remove(pessoa.getIdCadastro()));
        if (!naoEncontrados.isEmpty()) {
            throw new IllegalArgumentException(rotulo + " não encontrado(s): " + juntar(naoEncontrados));
        }

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

    private List<ConflitoHorarioDTO> buscarConflitos(Participantes participantes) {
        List<ConflitoHorarioDTO> conflitos = new ArrayList<>();
        terapiaRepository.buscarOcupacoesDeEspecialistas(
                participantes.idsEspecialistas(), participantes.inicio(), participantes.fim(), StatusTerapia.CANCELADA)
                .forEach(ocupacao -> conflitos.add(toConflitoDTO("ESPECIALISTA", ocupacao)));
        terapiaRepository.buscarOcupacoesDePacientes(
                participantes.idsPacientes(), participantes.inicio(), participantes.fim(), StatusTerapia.CANCELADA)
                .forEach(ocupacao -> conflitos.add(toConflitoDTO("PACIENTE", ocupacao)));
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

    private String juntar(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
