package com.fatec.gisa.controllers.especialista;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.especialista.request.EspecialidadePatchRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialidadeRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeDetalheDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeListaDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeResponseDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.services.especialista.EspecialidadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Gestão exige GERENCIAR_ESPECIALIDADES; a listagem basta estar logado (usada em vários formulários)
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_ESPECIALIDADES')")
public class EspecialidadeController {

    private final EspecialidadeService especialidadeService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<EspecialidadeListaDTO>> listarTodas() {
        return ResponseEntity.ok(especialidadeService.listarTodas());
    }

    @GetMapping("/{idEspecialidade}")
    public ResponseEntity<EspecialidadeDetalheDTO> buscarPorId(@PathVariable Long idEspecialidade) {
        return ResponseEntity.ok(especialidadeService.buscarDetalhes(idEspecialidade));
    }

    @PostMapping
    public ResponseEntity<EspecialidadeResponseDTO> criar(@Valid @RequestBody EspecialidadeRequestDTO dto) {
        Especialidade especialidade = especialidadeService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(especialidade));
    }

    @PatchMapping("/{idEspecialidade}")
    public ResponseEntity<EspecialidadeDetalheDTO> atualizar(
            @PathVariable Long idEspecialidade,
            @Valid @RequestBody EspecialidadePatchRequestDTO request) {
        especialidadeService.atualizar(idEspecialidade, request);
        return ResponseEntity.ok(especialidadeService.buscarDetalhes(idEspecialidade));
    }

    @DeleteMapping("/{idEspecialidade}")
    public ResponseEntity<Void> excluir(@PathVariable Long idEspecialidade) {
        especialidadeService.excluir(idEspecialidade);
        return ResponseEntity.noContent().build();
    }

    private EspecialidadeResponseDTO toResponse(Especialidade especialidade) {
        return new EspecialidadeResponseDTO(
                especialidade.getIdEspecialidade(),
                especialidade.getNome(),
                especialidade.getDescricao());
    }
}
