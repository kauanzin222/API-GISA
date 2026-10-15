package com.fatec.gisa.services.paciente;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.paciente.response.EventoPacienteDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.paciente.EventoPaciente;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.paciente.EventoPacienteRepository;
import com.fatec.gisa.repositories.paciente.PacienteRepository;

import lombok.RequiredArgsConstructor;

// Linha do tempo do paciente. Diferente da auditoria (que roda em transação própria e registra
// toda requisição), aqui os eventos são de negócio e gravados na MESMA transação da operação:
// se ela falhar, o evento também não fica gravado. As descrições nunca levam dado de saúde.
@Service
@RequiredArgsConstructor
public class HistoricoPacienteService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant SEM_INICIO = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant SEM_FIM = Instant.parse("9999-01-01T00:00:00Z");
    private static final int TAMANHO_DESCRICAO = 500;

    private final EventoPacienteRepository eventoPacienteRepository;
    private final PacienteRepository pacienteRepository;
    private final PessoaRepository pessoaRepository;
    private final AcessoPacienteService acessoPacienteService;

    // MANDATORY: só grava dentro da transação de quem gerou o evento, nunca sozinho
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Paciente paciente, TipoEventoPaciente tipo, String descricao) {
        registrar(List.of(paciente), tipo, descricao, null);
    }

    // Um evento por paciente (ex.: terapia coletiva), todos com o mesmo horário e autor
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Collection<Paciente> pacientes, TipoEventoPaciente tipo, String descricao,
            Long idTerapia) {
        if (pacientes.isEmpty()) {
            return;
        }
        Pessoa usuario = usuarioLogado();
        Instant agora = Instant.now();
        String texto = descricao.length() <= TAMANHO_DESCRICAO
                ? descricao
                : descricao.substring(0, TAMANHO_DESCRICAO - 1) + "…";
        eventoPacienteRepository.saveAll(pacientes.stream()
                .map(paciente -> new EventoPaciente(null, paciente, agora, tipo, texto, idTerapia, usuario))
                .toList());
    }

    // Datas no fuso de Brasília; "fim" inclui o dia inteiro
    @Transactional(readOnly = true)
    public Page<EventoPacienteDTO> listar(Long idPaciente, List<TipoEventoPaciente> tipos,
            LocalDate inicio, LocalDate fim, Pageable pageable) {
        // Quem só vê os pacientes vinculados recebe 403 para os demais
        acessoPacienteService.exigirAcesso(idPaciente);
        if (!pacienteRepository.existsById(idPaciente)) {
            throw new ResourceNotFoundException("Paciente não encontrado com ID: " + idPaciente);
        }
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial.");
        }
        Collection<TipoEventoPaciente> tiposFiltrados = tipos == null || tipos.isEmpty()
                ? EnumSet.allOf(TipoEventoPaciente.class)
                : tipos;

        // Eventos da mesma operação têm o mesmo horário: o ID desempata na ordem em que foram gravados
        Sort ordem = pageable.getSort().getOrderFor("idEvento") != null
                ? pageable.getSort()
                : pageable.getSort().and(Sort.by(Sort.Direction.DESC, "idEvento"));

        return eventoPacienteRepository.buscar(
                        idPaciente,
                        tiposFiltrados,
                        inicio == null ? SEM_INICIO : inicio.atStartOfDay(FUSO).toInstant(),
                        fim == null ? SEM_FIM : fim.plusDays(1).atStartOfDay(FUSO).toInstant(),
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordem))
                .map(evento -> new EventoPacienteDTO(
                        evento.getIdEvento(),
                        evento.getDataHora(),
                        evento.getTipo(),
                        evento.getDescricao(),
                        evento.getIdTerapia(),
                        evento.getUsuario() == null
                                ? null
                                : new EventoPacienteDTO.UsuarioEventoDTO(
                                        evento.getUsuario().getIdCadastro(), evento.getUsuario().getNome())));
    }

    private Pessoa usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return pessoaRepository.findByCpf(autenticacao.getName()).orElse(null);
    }
}
