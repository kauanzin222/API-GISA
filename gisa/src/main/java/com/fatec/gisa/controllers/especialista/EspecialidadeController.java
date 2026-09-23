package com.fatec.gisa.controllers.especialista;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.especialista.response.EspecialidadeResponseDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.services.especialista.EspecialidadeService;

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
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(especialidades);
    }

    private EspecialidadeResponseDTO toDTO(Especialidade especialidade) {
        return new EspecialidadeResponseDTO(
                especialidade.getIdEspecialidade(),
                especialidade.getNome(),
                especialidade.getDescricao());
    }
}