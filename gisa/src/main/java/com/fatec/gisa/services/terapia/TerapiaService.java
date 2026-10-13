package com.fatec.gisa.services.terapia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.terapia.FiltroTerapiaDTO;
import com.fatec.gisa.dtos.terapia.request.PresencaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.StatusTerapiaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaPatchRequestDTO;
import com.fatec.gisa.dtos.terapia.request.VerificacaoConflitoRequestDTO;
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
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;
import com.fatec.gisa.repositories.terapia.OcupacaoHorario;
import com.fatec.gisa.repositories.terapia.TerapiaRepository;
import com.fatec.gisa.services.BuscaPorIds;
import com.fatec.gisa.services.paciente.HistoricoPacienteService;
import com.fatec.gisa.services.paciente.TextoHistorico;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TerapiaService {

    private static final String TITULO_COLETIVA = "Terapia coletiva";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Long NENHUMA_TERAPIA = 0L;
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final Map<StatusTerapia, List<StatusTerapia>> TRANSICOES = Map.of(
            StatusTerapia.AGENDADA, List.of(StatusTerapia.EM_ATENDIMENTO, StatusTerapia.CANCELADA),
            StatusTerapia.EM_ATENDIMENTO, List.of(StatusTerapia.CONCLUIDA, StatusTerapia.CANCELADA),
            StatusTerapia.CONCLUIDA, List.of(),
            StatusTerapia.CANCELADA, List.of());
    private static final Map<StatusTerapia, String> ROTULOS = Map.of(
            StatusTerapia.AGENDADA, "agendada",
            StatusTerapia.EM_ATENDIMENTO, "em atendimento",
            StatusTerapia.CONCLUIDA, "concluída",
            StatusTerapia.CANCELADA, "cancelada");
    // O servidor (Render) roda em UTC; "hoje" para a presença é o dia em Sorocaba
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final TerapiaRepository terapiaRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialistaRepository especialistaRepository;
    private final HistoricoPacienteService historicoPacienteService;

    @Transactional
    public Terapia cadastrar(TerapiaCadastroRequestDTO dto) {
        Participantes participantes = validarParticipantes(dto.pacientesIds(), dto.especialistasIds(),
                dto.data(), dto.horaInicio(), dto.horaTermino());
        bloquearEVerificarHorario(participantes, NENHUMA_TERAPIA);

        Terapia terapia = new Terapia();
        terapia.setDataHoraInicio(participantes.inicio());
        terapia.setDataHoraFim(participantes.fim());
        terapia.setModalidade(modalidade(participantes));
        terapia.setStatus(StatusTerapia.AGENDADA);
        terapia.setEspecialistas(new ArrayList<>(participantes.especialistas()));
        terapia.setPresencaPacientes(new ArrayList<>());
        Terapia terapiaSalva = terapiaRepository.save(terapia);

        for (Paciente paciente : participantes.pacientes()) {
            terapiaSalva.getPresencaPacientes().add(novaPresenca(terapiaSalva, paciente));
        }

        Terapia terapiaCadastrada = terapiaRepository.save(terapiaSalva);
        historicoPacienteService.registrar(participantes.pacientes(), TipoEventoPaciente.TERAPIA_AGENDADA,
                "%s agendada para %s, com %s.".formatted(
                        terapiaCadastrada.getModalidade() == Modalidade.COLETIVA ? "Terapia coletiva" : "Terapia",
                        horario(terapiaCadastrada), especialistas(terapiaCadastrada)),
                terapiaCadastrada.getIdTerapia());
        return terapiaCadastrada;
    }

    // Remarcar e trocar participantes. Só terapias agendadas; revalida tudo como no cadastro,
    // e a própria terapia não conta como conflito. Quem continua mantém presença e justificativa.
    @Transactional
    public Terapia atualizar(Long idTerapia, TerapiaPatchRequestDTO dto) {
        Terapia terapia = buscarParaAlterar(idTerapia);
        if (terapia.getStatus() != StatusTerapia.AGENDADA) {
            throw new IllegalArgumentException("Só é possível editar terapias agendadas. Esta terapia está "
                    + ROTULOS.get(terapia.getStatus()) + ".");
        }

        LocalDate data = valorEnviado(dto, "data", dto.getData(), terapia.getDataHoraInicio().toLocalDate(),
                "A data não pode ser nula.");
        LocalTime horaInicio = valorEnviado(dto, "horaInicio", dto.getHoraInicio(),
                terapia.getDataHoraInicio().toLocalTime(), "A hora de início não pode ser nula.");
        LocalTime horaTermino = valorEnviado(dto, "horaTermino", dto.getHoraTermino(),
                terapia.getDataHoraFim().toLocalTime(), "A hora de término não pode ser nula.");
        List<Long> pacientesIds = listaEnviada(dto, "pacientesIds", dto.getPacientesIds(),
                terapia.getPresencaPacientes().stream().map(presenca -> presenca.getPaciente().getIdCadastro()).toList(),
                "A terapia deve ter ao menos um paciente.");
        List<Long> especialistasIds = listaEnviada(dto, "especialistasIds", dto.getEspecialistasIds(),
                terapia.getEspecialistas().stream().map(Especialista::getIdCadastro).toList(),
                "A terapia deve ter ao menos um especialista.");

        Participantes participantes = validarParticipantes(
                pacientesIds, especialistasIds, data, horaInicio, horaTermino);
        bloquearEVerificarHorario(participantes, terapia.getIdTerapia());

        // Para o histórico
        String horarioAnterior = horario(terapia);
        String especialistasAnteriores = especialistas(terapia);
        Modalidade modalidadeAnterior = terapia.getModalidade();
        List<Paciente> pacientesAnteriores = terapia.getPresencaPacientes().stream()
                .map(PresencaPaciente::getPaciente)
                .toList();

        terapia.setDataHoraInicio(participantes.inicio());
        terapia.setDataHoraFim(participantes.fim());
        terapia.setModalidade(modalidade(participantes));
        terapia.getEspecialistas().clear();
        terapia.getEspecialistas().addAll(participantes.especialistas());

        Set<Long> idsPacientes = Set.copyOf(participantes.idsPacientes());
        terapia.getPresencaPacientes().removeIf(
                presenca -> !idsPacientes.contains(presenca.getPaciente().getIdCadastro()));
        Set<Long> idsQueContinuam = terapia.getPresencaPacientes().stream()
                .map(presenca -> presenca.getPaciente().getIdCadastro())
                .collect(Collectors.toSet());
        for (Paciente paciente : participantes.pacientes()) {
            if (!idsQueContinuam.contains(paciente.getIdCadastro())) {
                terapia.getPresencaPacientes().add(novaPresenca(terapia, paciente));
            }
        }

        Terapia terapiaAtualizada = terapiaRepository.save(terapia);
        registrarEdicao(terapiaAtualizada, horarioAnterior, especialistasAnteriores, modalidadeAnterior,
                pacientesAnteriores, participantes.pacientes());
        return terapiaAtualizada;
    }

    // Quem continua: TERAPIA_ALTERADA com o que mudou; quem entrou: TERAPIA_AGENDADA;
    // quem saiu: TERAPIA_ALTERADA. Os nomes dos outros pacientes nunca aparecem (LGPD).
    private void registrarEdicao(Terapia terapia, String horarioAnterior, String especialistasAnteriores,
            Modalidade modalidadeAnterior, List<Paciente> pacientesAnteriores, List<Paciente> pacientesAtuais) {
        Set<Long> idsAnteriores = pacientesAnteriores.stream().map(Paciente::getIdCadastro).collect(Collectors.toSet());
        Set<Long> idsAtuais = pacientesAtuais.stream().map(Paciente::getIdCadastro).collect(Collectors.toSet());
        Long idTerapia = terapia.getIdTerapia();

        List<String> mudancas = new ArrayList<>();
        if (!horario(terapia).equals(horarioAnterior)) {
            mudancas.add("remarcada para " + horario(terapia));
        }
        if (!especialistas(terapia).equals(especialistasAnteriores)) {
            mudancas.add("especialistas: " + especialistas(terapia));
        }
        if (!idsAtuais.equals(idsAnteriores)) {
            mudancas.add(terapia.getModalidade() == modalidadeAnterior
                    ? "pacientes da terapia alterados"
                    : "agora " + (terapia.getModalidade() == Modalidade.COLETIVA ? "coletiva" : "individual"));
        }
        if (!mudancas.isEmpty()) {
            historicoPacienteService.registrar(
                    pacientesAtuais.stream().filter(paciente -> idsAnteriores.contains(paciente.getIdCadastro())).toList(),
                    TipoEventoPaciente.TERAPIA_ALTERADA,
                    "Terapia de %s, alterada: %s.".formatted(horarioAnterior, String.join("; ", mudancas)),
                    idTerapia);
        }
        historicoPacienteService.registrar(
                pacientesAtuais.stream().filter(paciente -> !idsAnteriores.contains(paciente.getIdCadastro())).toList(),
                TipoEventoPaciente.TERAPIA_AGENDADA,
                "Paciente incluído na terapia de %s, com %s.".formatted(horario(terapia), especialistas(terapia)),
                idTerapia);
        historicoPacienteService.registrar(
                pacientesAnteriores.stream().filter(paciente -> !idsAtuais.contains(paciente.getIdCadastro())).toList(),
                TipoEventoPaciente.TERAPIA_ALTERADA,
                "Paciente retirado da terapia de %s, com %s.".formatted(horarioAnterior, especialistasAnteriores),
                idTerapia);
    }

    // Ex.: "27/10/2026, das 09:00 às 10:00"
    private String horario(Terapia terapia) {
        return TextoHistorico.horario(terapia.getDataHoraInicio(), terapia.getDataHoraFim());
    }

    // Ex.: "Ana Souza e Carlos Lima"
    private String especialistas(Terapia terapia) {
        return TextoHistorico.lista(terapia.getEspecialistas().stream().map(Especialista::getNome).sorted().toList());
    }

    private List<Paciente> pacientesDa(Terapia terapia) {
        return terapia.getPresencaPacientes().stream().map(PresencaPaciente::getPaciente).toList();
    }

    // Campo omitido mantém o valor atual; enviado como null é recusado (é obrigatório)
    private <T> T valorEnviado(TerapiaPatchRequestDTO dto, String campo, T enviado, T atual, String mensagemNulo) {
        if (!dto.hasField(campo)) {
            return atual;
        }
        if (enviado == null) {
            throw new IllegalArgumentException(mensagemNulo);
        }
        return enviado;
    }

    private List<Long> listaEnviada(TerapiaPatchRequestDTO dto, String campo, List<Long> enviada,
            List<Long> atual, String mensagemVazia) {
        List<Long> lista = valorEnviado(dto, campo, enviada, atual, mensagemVazia);
        if (lista.isEmpty()) {
            throw new IllegalArgumentException(mensagemVazia);
        }
        return lista;
    }

    // A trava precisa vir antes da verificação: um cadastro ou edição concorrente com alguém em
    // comum espera esta transação terminar e, ao verificar, já enxerga esta terapia.
    private void bloquearEVerificarHorario(Participantes participantes, Long idTerapiaIgnorada) {
        List<Long> idsEnvolvidos = new ArrayList<>(participantes.idsPacientes());
        idsEnvolvidos.addAll(participantes.idsEspecialistas());
        terapiaRepository.bloquearPessoas(idsEnvolvidos);

        List<ConflitoHorarioDTO> conflitos = buscarConflitos(participantes, idTerapiaIgnorada);
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException(mensagemDeConflito(conflitos));
        }
    }

    private Modalidade modalidade(Participantes participantes) {
        return participantes.pacientes().size() > 1 ? Modalidade.COLETIVA : Modalidade.INDIVIDUAL;
    }

    private PresencaPaciente novaPresenca(Terapia terapia, Paciente paciente) {
        PresencaPaciente presenca = new PresencaPaciente();
        presenca.setTerapia(terapia);
        presenca.setPaciente(paciente);
        presenca.setPresenca(StatusPresenca.PENDENTE);
        return presenca;
    }

    // AGENDADA → EM_ATENDIMENTO → CONCLUIDA; AGENDADA ou EM_ATENDIMENTO → CANCELADA.
    // CONCLUIDA e CANCELADA são finais. Terapia cancelada deixa de ocupar horário.
    @Transactional
    public Terapia alterarStatus(Long idTerapia, StatusTerapiaRequestDTO dto) {
        Terapia terapia = buscarParaAlterar(idTerapia);
        StatusTerapia atual = terapia.getStatus();
        StatusTerapia novo = dto.status();

        if (TRANSICOES.get(atual).isEmpty()) {
            throw new IllegalArgumentException(
                    "A terapia está " + ROTULOS.get(atual) + " e não pode mais mudar de status.");
        }
        if (atual == novo) {
            throw new IllegalArgumentException("A terapia já está com o status " + novo + ".");
        }
        if (!TRANSICOES.get(atual).contains(novo)) {
            throw new IllegalArgumentException("Não é possível alterar o status de " + atual + " para " + novo
                    + ". A partir de " + atual + ", os status permitidos são: "
                    + TRANSICOES.get(atual).stream().map(Enum::name).collect(Collectors.joining(", ")) + ".");
        }

        String motivo = null;
        if (novo == StatusTerapia.CANCELADA) {
            if (dto.motivo() == null || dto.motivo().isBlank()) {
                throw new IllegalArgumentException("Informe o motivo do cancelamento.");
            }
            motivo = dto.motivo().trim();
        }

        terapia.setStatus(novo);
        terapia.setMotivoCancelamento(motivo);
        Terapia terapiaAtualizada = terapiaRepository.save(terapia);

        // O motivo é texto livre e pode conter dado de saúde: não vai para o histórico.
        // Iniciar e concluir o atendimento não geram evento (não fazem parte da lista de eventos).
        if (novo == StatusTerapia.CANCELADA) {
            historicoPacienteService.registrar(pacientesDa(terapiaAtualizada), TipoEventoPaciente.TERAPIA_CANCELADA,
                    "Terapia de %s, com %s, cancelada.".formatted(
                            horario(terapiaAtualizada), especialistas(terapiaAtualizada)),
                    terapiaAtualizada.getIdTerapia());
        }
        return terapiaAtualizada;
    }

    // Trava a linha da terapia até o fim da transação: mudanças simultâneas na mesma terapia
    // (ex.: cancelar enquanto outro usuário edita) são aplicadas uma depois da outra
    private Terapia buscarParaAlterar(Long idTerapia) {
        return terapiaRepository.buscarParaAlterar(idTerapia)
                .orElseThrow(() -> new ResourceNotFoundException("Terapia não encontrada com ID: " + idTerapia));
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

        StatusPresenca anterior = presenca.getPresenca();
        presenca.setPresenca(nova);
        presenca.setJustificativa(justificativa);
        Terapia terapiaAtualizada = terapiaRepository.save(terapia);

        // Só no histórico do paciente marcado; a justificativa (texto livre) não entra
        if (anterior != nova) {
            historicoPacienteService.registrar(List.of(presenca.getPaciente()), TipoEventoPaciente.PRESENCA_REGISTRADA,
                    "Presença registrada na terapia de %s: %s.".formatted(
                            horario(terapiaAtualizada), TextoHistorico.presenca(nova)),
                    terapiaAtualizada.getIdTerapia());
        }
        return terapiaAtualizada;
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
    public VerificacaoConflitoDTO verificarConflitos(VerificacaoConflitoRequestDTO dto) {
        Long idTerapiaIgnorada = NENHUMA_TERAPIA;
        if (dto.idTerapiaIgnorada() != null) {
            // ID enviado no corpo: inexistente torna o pedido inválido (400), não a rota (404)
            if (!terapiaRepository.existsById(dto.idTerapiaIgnorada())) {
                throw new IllegalArgumentException("Terapia não encontrada com ID: " + dto.idTerapiaIgnorada());
            }
            idTerapiaIgnorada = dto.idTerapiaIgnorada();
        }
        Participantes participantes = validarParticipantes(dto.pacientesIds(), dto.especialistasIds(),
                dto.data(), dto.horaInicio(), dto.horaTermino());
        List<ConflitoHorarioDTO> conflitos = buscarConflitos(participantes, idTerapiaIgnorada);
        return new VerificacaoConflitoDTO(!conflitos.isEmpty(), conflitos);
    }

    @Transactional(readOnly = true)
    public Page<TerapiaListaDTO> listarPaginado(FiltroTerapiaDTO filtro, Pageable pageable) {
        return buscar(filtro, pageable).map(this::toListaDTO);
    }

    private Page<Terapia> buscar(FiltroTerapiaDTO filtro, Pageable pageable) {
        // A tela do dia pede todas as terapias de uma vez; acima disso a página fica pesada demais
        if (pageable.getPageSize() > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException(
                    "O parâmetro 'size' deve ser no máximo " + TAMANHO_MAXIMO_PAGINA + ".");
        }
        return terapiaRepository.buscar(filtro.status(), filtro.inicio(), filtro.fim(),
                filtro.idPaciente(), filtro.idEspecialista(), pageable);
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
            String cpf, FiltroTerapiaDTO filtro, Pageable pageable) {
        Especialista especialista = especialistaRepository.findByCpf(cpf)
                .orElseThrow(() -> new IllegalArgumentException(
                        "O usuário logado não é um especialista."));
        Long idEspecialista = especialista.getIdCadastro();

        Page<Terapia> pagina = buscar(filtro.comEspecialista(idEspecialista), pageable);

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
                        .toList(),
                terapia.getMotivoCancelamento()));
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

    private Participantes validarParticipantes(List<Long> pacientesIds, List<Long> especialistasIds,
            LocalDate data, LocalTime horaInicio, LocalTime horaTermino) {
        if (!horaTermino.isAfter(horaInicio)) {
            throw new IllegalArgumentException("A hora de término deve ser posterior à hora de início.");
        }

        List<Paciente> pacientes = buscarAtivos(
                pacientesIds, pacienteRepository::findAllById, "Paciente(s)");
        List<Especialista> especialistas = buscarAtivos(
                especialistasIds, especialistaRepository::findAllById, "Especialista(s)");

        return new Participantes(
                pacientes,
                especialistas,
                data.atTime(horaInicio),
                data.atTime(horaTermino));
    }

    // Terapia nova: NENHUMA_TERAPIA, já que ainda não existe e não há o que ignorar
    private List<ConflitoHorarioDTO> buscarConflitos(Participantes participantes, Long idTerapiaIgnorada) {
        return buscarConflitos(participantes.idsPacientes(), participantes.idsEspecialistas(),
                participantes.inicio(), participantes.fim(), idTerapiaIgnorada);
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
                        .toList(),
                terapia.getMotivoCancelamento());
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
