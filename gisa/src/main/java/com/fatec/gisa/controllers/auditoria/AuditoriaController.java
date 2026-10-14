package com.fatec.gisa.controllers.auditoria;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.auditoria.LogAuditoriaDTO;
import com.fatec.gisa.enums.AcaoAuditoria;
import com.fatec.gisa.services.auditoria.AuditoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_AUDITORIA')")
    @GetMapping
    public ResponseEntity<Page<LogAuditoriaDTO>> listar(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) List<AcaoAuditoria> acao,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @PageableDefault(page = 0, size = 20, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.listar(cpf, acao, inicio, fim, pageable));
    }
}
