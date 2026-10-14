package com.fatec.gisa.controllers.usuario;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.usuario.response.PermissaoResumoDTO;
import com.fatec.gisa.services.usuario.PermissaoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permissoes")
// Todas as rotas exigem GERENCIAR_PERFIS
@PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_PERFIS')")
public class PermissaoController {

    private final PermissaoService permissaoService;

    @GetMapping
    public ResponseEntity<List<PermissaoResumoDTO>> listarTodas() {
        return ResponseEntity.ok(permissaoService.listarTodas());
    }

    @GetMapping("/{idPermissao}")
    public ResponseEntity<PermissaoResumoDTO> buscarPorId(@PathVariable Long idPermissao) {
        return ResponseEntity.ok(permissaoService.buscarPorId(idPermissao));
    }
}
