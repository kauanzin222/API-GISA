package com.fatec.gisa.controllers.paciente;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.paciente.response.CidResponseDTO;
import com.fatec.gisa.entities.paciente.Cid;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.services.paciente.CidService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cids")
@RequiredArgsConstructor
public class CidController {

    private final CidService cidService;

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_PACIENTE', 'EDITAR_PACIENTE')")
    @GetMapping("/{codigoCID}")
    public ResponseEntity<CidResponseDTO> buscarPorCodigo(@PathVariable String codigoCID) {
        return ResponseEntity.ok(cidService.buscarPorCodigo(codigoCID)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CID não encontrado: " + codigoCID)));
    }

    private CidResponseDTO toResponse(Cid cid) {
        return new CidResponseDTO(cid.getCodigoCID(), cid.getDescricao());
    }
}
