package com.fatec.gisa.controllers.terapia;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.terapia.FiltroTerapiaDTO;
import com.fatec.gisa.dtos.terapia.request.PresencaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.StatusTerapiaRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaCadastroRequestDTO;
import com.fatec.gisa.dtos.terapia.request.TerapiaPatchRequestDTO;
import com.fatec.gisa.dtos.terapia.request.VerificacaoConflitoRequestDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaDoEspecialistaDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaListaDTO;
import com.fatec.gisa.dtos.terapia.response.VerificacaoConflitoDTO;
import com.fatec.gisa.entities.terapia.Terapia;
import com.fatec.gisa.enums.StatusTerapia;
import com.fatec.gisa.services.terapia.TerapiaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/terapias")
@RequiredArgsConstructor
public class TerapiaController {

    private final TerapiaService terapiaService;

    // Filtros opcionais e combináveis; "data" não pode ser usado junto com "inicio"/"fim"
    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_TERAPIAS')")
    @GetMapping
    public ResponseEntity<Page<TerapiaListaDTO>> listarPaginado(
            @RequestParam(required = false) List<StatusTerapia> status,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long idPaciente,
            @RequestParam(required = false) Long idEspecialista,
            @PageableDefault(page = 0, size = 6, sort = "dataHoraInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(terapiaService.listarPaginado(
                FiltroTerapiaDTO.de(status, data, inicio, fim, idPaciente, idEspecialista), pageable));
    }

    // Terapias do especialista logado (identificado pelo CPF do token). Sem permissão específica:
    // basta estar logado, e o serviço recusa quem não é especialista
    @GetMapping("/minhas")
    public ResponseEntity<Page<TerapiaDoEspecialistaDTO>> listarDoEspecialistaLogado(
            Authentication authentication,
            @RequestParam(required = false) List<StatusTerapia> status,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long idPaciente,
            @PageableDefault(page = 0, size = 3, sort = "dataHoraInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(terapiaService.listarDoEspecialista(authentication.getName(),
                FiltroTerapiaDTO.de(status, data, inicio, fim, idPaciente, null), pageable));
    }

    // O especialista não tem VISUALIZAR_TERAPIAS, mas abre as terapias de que participa (prontuário)
    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_TERAPIAS')"
            + " or @terapiaService.especialistaParticipa(#idTerapia, authentication.name)")
    @GetMapping("/{idTerapia}")
    public ResponseEntity<TerapiaListaDTO> buscarPorId(@PathVariable Long idTerapia) {
        return ResponseEntity.ok(terapiaService.buscarDetalhes(idTerapia));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'REGISTRAR_PRESENCA')")
    @PatchMapping("/{idTerapia}/pacientes/{idPaciente}/presenca")
    public ResponseEntity<TerapiaListaDTO> registrarPresenca(
            @PathVariable Long idTerapia,
            @PathVariable Long idPaciente,
            @Valid @RequestBody PresencaRequestDTO request) {
        terapiaService.registrarPresenca(idTerapia, idPaciente, request);
        return ResponseEntity.ok(terapiaService.buscarDetalhes(idTerapia));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'ALTERAR_STATUS_TERAPIA')")
    @PatchMapping("/{idTerapia}/status")
    public ResponseEntity<TerapiaListaDTO> alterarStatus(
            @PathVariable Long idTerapia,
            @Valid @RequestBody StatusTerapiaRequestDTO request) {
        terapiaService.alterarStatus(idTerapia, request);
        return ResponseEntity.ok(terapiaService.buscarDetalhes(idTerapia));
    }

    // Remarcar a terapia e trocar participantes (só terapias agendadas)
    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_TERAPIA')")
    @PatchMapping("/{idTerapia}")
    public ResponseEntity<TerapiaListaDTO> atualizar(
            @PathVariable Long idTerapia,
            @Valid @RequestBody TerapiaPatchRequestDTO request) {
        terapiaService.atualizar(idTerapia, request);
        return ResponseEntity.ok(terapiaService.buscarDetalhes(idTerapia));
    }

    // Não grava nada: responde se o horário conflita com outras terapias
    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'AGENDAR_TERAPIA', 'EDITAR_TERAPIA')")
    @PostMapping("/verificar-conflitos")
    public ResponseEntity<VerificacaoConflitoDTO> verificarConflitos(
            @Valid @RequestBody VerificacaoConflitoRequestDTO request) {
        return ResponseEntity.ok(terapiaService.verificarConflitos(request));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'AGENDAR_TERAPIA', 'EDITAR_TERAPIA')")
    @PostMapping
    public ResponseEntity<TerapiaListaDTO> cadastrar(@Valid @RequestBody TerapiaCadastroRequestDTO request) {
        Terapia terapia = terapiaService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(terapiaService.buscarDetalhes(terapia.getIdTerapia()));
    }
}
