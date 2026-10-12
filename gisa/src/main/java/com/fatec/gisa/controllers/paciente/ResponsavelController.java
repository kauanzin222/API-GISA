package com.fatec.gisa.controllers.paciente;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.paciente.request.ResponsavelPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelDetalheDTO;
import com.fatec.gisa.services.paciente.ResponsavelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/responsaveis")
@RequiredArgsConstructor
public class ResponsavelController {

    private final ResponsavelService responsavelService;

    @GetMapping("/{idResponsavel}")
    public ResponseEntity<ResponsavelDetalheDTO> buscarPorId(
            @PathVariable Long idResponsavel) {
        return ResponseEntity.ok(responsavelService.buscarDetalhes(idResponsavel));
    }

    @PatchMapping("/{idResponsavel}")
    public ResponseEntity<ResponsavelDetalheDTO> atualizar(
            @PathVariable Long idResponsavel,
            @Valid @RequestBody ResponsavelPatchRequestDTO request) {
        return ResponseEntity.ok(responsavelService.atualizar(idResponsavel, request));
    }

    @DeleteMapping("/{idResponsavel}")
    public ResponseEntity<Void> inativar(@PathVariable Long idResponsavel) {
        responsavelService.inativar(idResponsavel);
        return ResponseEntity.noContent().build();
    }
}
