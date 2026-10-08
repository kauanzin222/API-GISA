package com.fatec.gisa.controllers.paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.paciente.request.PacienteCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteCadastroResponseDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteResumoDTO;
import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;
import com.fatec.gisa.services.paciente.PacienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final CadastroFacadeService cadastroFacadeService;
    private final PacienteService pacienteService;

    @GetMapping
    public ResponseEntity<Page<PacienteResumoDTO>> listarPaginado(
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(pacienteService.listarPaginado(pageable));
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