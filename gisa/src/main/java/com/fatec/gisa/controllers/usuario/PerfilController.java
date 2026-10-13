package com.fatec.gisa.controllers.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.usuario.request.PerfilPatchRequestDTO;
import com.fatec.gisa.dtos.usuario.request.PerfilRequestDTO;
import com.fatec.gisa.dtos.usuario.request.PermissoesRequestDTO;
import com.fatec.gisa.dtos.usuario.response.PerfilDetalheDTO;
import com.fatec.gisa.dtos.usuario.response.PerfilListaDTO;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.services.usuario.PerfilService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/perfis")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    public ResponseEntity<List<PerfilListaDTO>> listarTodos() {
        return ResponseEntity.ok(perfilService.listarTodos());
    }

    @GetMapping("/{idPerfil}")
    public ResponseEntity<PerfilDetalheDTO> buscarPorId(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(perfilService.buscarDetalhes(idPerfil));
    }

    @PostMapping
    public ResponseEntity<PerfilDetalheDTO> criar(@Valid @RequestBody PerfilRequestDTO request) {
        Perfil perfil = perfilService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perfilService.buscarDetalhes(perfil.getIdPerfil()));
    }

    @PatchMapping("/{idPerfil}")
    public ResponseEntity<PerfilDetalheDTO> atualizar(
            @PathVariable Long idPerfil,
            @Valid @RequestBody PerfilPatchRequestDTO request) {
        perfilService.atualizar(idPerfil, request);
        return ResponseEntity.ok(perfilService.buscarDetalhes(idPerfil));
    }

    @DeleteMapping("/{idPerfil}")
    public ResponseEntity<Void> excluir(@PathVariable Long idPerfil) {
        perfilService.excluir(idPerfil);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idPerfil}/permissoes")
    public ResponseEntity<PerfilDetalheDTO> adicionarPermissoes(
            @PathVariable Long idPerfil,
            @Valid @RequestBody PermissoesRequestDTO request) {
        perfilService.adicionarPermissoes(idPerfil, request.permissoesIds());
        return ResponseEntity.ok(perfilService.buscarDetalhes(idPerfil));
    }

    @DeleteMapping("/{idPerfil}/permissoes/{idPermissao}")
    public ResponseEntity<PerfilDetalheDTO> removerPermissao(
            @PathVariable Long idPerfil,
            @PathVariable Long idPermissao) {
        perfilService.removerPermissao(idPerfil, idPermissao);
        return ResponseEntity.ok(perfilService.buscarDetalhes(idPerfil));
    }
}
