package com.fatec.gisa.controllers.paciente;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.PacientePatchRequestDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.response.EventoPacienteDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteCadastroResponseDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteResumoDTO;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.enums.TipoEventoPaciente;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;
import com.fatec.gisa.services.paciente.HistoricoPacienteService;
import com.fatec.gisa.services.paciente.PacienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final CadastroFacadeService cadastroFacadeService;
    private final PacienteService pacienteService;
    private final HistoricoPacienteService historicoPacienteService;

    @GetMapping
    public ResponseEntity<Page<PacienteResumoDTO>> listarPaginado(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String nome,
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(pacienteService.listarPaginado(FiltroPessoaDTO.de(cpf, nome), pageable));
    }

    @GetMapping("/{idCadastro}")
    public ResponseEntity<PacienteDetalheDTO> buscarPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.ok(pacienteService.buscarDetalhes(idCadastro));
    }

    // Linha do tempo do paciente (eventos de negócio, sem dado de saúde); não é a auditoria
    @GetMapping("/{idCadastro}/historico")
    public ResponseEntity<Page<EventoPacienteDTO>> listarHistorico(
            @PathVariable Long idCadastro,
            @RequestParam(required = false) List<TipoEventoPaciente> tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fim,
            @PageableDefault(page = 0, size = 20, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(historicoPacienteService.listar(idCadastro, tipo, inicio, fim, pageable));
    }

    @PatchMapping("/{idCadastro}")
    public ResponseEntity<PacienteDetalheDTO> atualizar(
            @PathVariable Long idCadastro,
            @Valid @RequestBody PacientePatchRequestDTO request) {
        pacienteService.atualizar(idCadastro, request);
        return ResponseEntity.ok(pacienteService.buscarDetalhes(idCadastro));
    }

    @PostMapping("/{idCadastro}/responsaveis")
    public ResponseEntity<PacienteDetalheDTO> adicionarResponsavel(
            @PathVariable Long idCadastro,
            @Valid @RequestBody ResponsavelCadastroRequestDTO request) {
        pacienteService.adicionarResponsavel(idCadastro, request);
        return ResponseEntity.status(201).body(pacienteService.buscarDetalhes(idCadastro));
    }

    @DeleteMapping("/{idCadastro}")
    public ResponseEntity<Void> inativar(@PathVariable Long idCadastro) {
        pacienteService.inativar(idCadastro);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<PacienteCadastroResponseDTO> cadastrar(
            @Valid @RequestBody PacienteCadastroRequestDTO request) {
        Paciente paciente = cadastroFacadeService.cadastrarPaciente(request);
        PacienteCadastroResponseDTO response = new PacienteCadastroResponseDTO(
                paciente.getIdCadastro(),
                paciente.getNome(),
                paciente.getCpf(),
                paciente.getStatusPaciente(),
                paciente.getDataCadastro());
        return ResponseEntity.status(201).body(response);
    }
}