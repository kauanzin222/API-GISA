package com.fatec.gisa.controllers.profissional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalComUsuarioRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalCadastroResponseDTO;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

    private final CadastroFacadeService cadastroFacadeService;

    @PostMapping
    public ResponseEntity<ProfissionalCadastroResponseDTO> cadastrar(
            @Valid @RequestBody ProfissionalCadastroRequestDTO request) {
        Profissional profissional = cadastroFacadeService.cadastrarProfissional(request);
        return ResponseEntity.status(201).body(toResponse(profissional, false));
    }

    @PostMapping("/com-usuario")
    public ResponseEntity<ProfissionalCadastroResponseDTO> cadastrarComUsuario(
            @Valid @RequestBody ProfissionalComUsuarioRequestDTO request) {
        Profissional profissional = cadastroFacadeService.cadastrarProfissionalComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(profissional, true));
    }

    private ProfissionalCadastroResponseDTO toResponse(Profissional profissional, boolean acessoCriado) {
        return new ProfissionalCadastroResponseDTO(
                profissional.getIdCadastro(),
                profissional.getNome(),
                profissional.getCpf(),
                profissional.getEmail(),
                profissional.getCargo().getIdCargo(),
                acessoCriado);
    }
}
