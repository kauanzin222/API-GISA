package com.fatec.gisa.controllers.usuario;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.usuario.response.PerfilResponseDTO;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.services.usuario.PerfilService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/perfis")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    public ResponseEntity<List<PerfilResponseDTO>> listarTodos() {
        List<PerfilResponseDTO> perfis = perfilService.listarTodos()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(perfis);
    }

    private PerfilResponseDTO toResponse(Perfil perfil) {
        return new PerfilResponseDTO(
                perfil.getIdPerfil(),
                perfil.getNome());
    }
}
