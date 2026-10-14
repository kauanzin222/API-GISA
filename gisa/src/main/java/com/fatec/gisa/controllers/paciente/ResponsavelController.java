package com.fatec.gisa.controllers.paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.paciente.request.ResponsavelPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.ResponsavelListaDTO;
import com.fatec.gisa.services.paciente.ResponsavelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/responsaveis")
@RequiredArgsConstructor
public class ResponsavelController {

    private final ResponsavelService responsavelService;

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PACIENTES')")
    @GetMapping
    public ResponseEntity<Page<ResponsavelListaDTO>> listarPaginado(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String nome,
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(responsavelService.listarPaginado(FiltroPessoaDTO.de(cpf, nome), pageable));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PACIENTES')")
    @GetMapping("/{idResponsavel}")
    public ResponseEntity<ResponsavelDetalheDTO> buscarPorId(
            @PathVariable Long idResponsavel) {
        return ResponseEntity.ok(responsavelService.buscarDetalhes(idResponsavel));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PACIENTE')")
    @PatchMapping("/{idResponsavel}")
    public ResponseEntity<ResponsavelDetalheDTO> atualizar(
            @PathVariable Long idResponsavel,
            @Valid @RequestBody ResponsavelPatchRequestDTO request) {
        return ResponseEntity.ok(responsavelService.atualizar(idResponsavel, request));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'INATIVAR_PACIENTE')")
    @DeleteMapping("/{idResponsavel}")
    public ResponseEntity<Void> inativar(@PathVariable Long idResponsavel) {
        responsavelService.inativar(idResponsavel);
        return ResponseEntity.noContent().build();
    }
}
