package com.fatec.gisa.controllers.especialista;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.especialista.request.EspecialidadeRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeResponseDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.services.especialista.EspecialidadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadeController {

    private final EspecialidadeService especialidadeService;

    @GetMapping
    public ResponseEntity<List<EspecialidadeResponseDTO>> listarTodas() {
        List<EspecialidadeResponseDTO> especialidades = especialidadeService.listarTodas()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(especialidades);
    }

    @PostMapping
    public ResponseEntity<EspecialidadeResponseDTO> criar(@Valid @RequestBody EspecialidadeRequestDTO dto) {
        Especialidade especialidade = especialidadeService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(especialidade));
    }

    private EspecialidadeResponseDTO toResponse(Especialidade especialidade) {
        return new EspecialidadeResponseDTO(
                especialidade.getIdEspecialidade(),
                especialidade.getNome(),
                especialidade.getDescricao());
    }
}