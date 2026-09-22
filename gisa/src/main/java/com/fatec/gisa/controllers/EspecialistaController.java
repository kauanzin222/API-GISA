package com.fatec.gisa.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaCadastroResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJCadastroResponseDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/especialistas")
@RequiredArgsConstructor
public class EspecialistaController {

    private final CadastroFacadeService cadastroFacadeService;

    @PostMapping
    public ResponseEntity<EspecialistaCadastroResponseDTO> cadastrar(
            @Valid @RequestBody EspecialistaCadastroRequestDTO request) {
        Especialista especialista = cadastroFacadeService.cadastrarEspecialista(request);
        return ResponseEntity.status(201).body(toResponse(especialista, false));
    }

    @PostMapping("/com-usuario")
    public ResponseEntity<EspecialistaCadastroResponseDTO> cadastrarComUsuario(
            @Valid @RequestBody EspecialistaComUsuarioRequestDTO request) {
        Especialista especialista = cadastroFacadeService.cadastrarEspecialistaComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(especialista, true));
    }

    @PostMapping("/pj")
    public ResponseEntity<EspecialistaPJCadastroResponseDTO> cadastrarPJ(
            @Valid @RequestBody EspecialistaPJCadastroRequestDTO request) {
        EspecialistaPJ especialistaPJ = cadastroFacadeService.cadastrarEspecialistaPJ(request);
        return ResponseEntity.status(201).body(toResponse(especialistaPJ, false));
    }

    @PostMapping("/pj/com-usuario")
    public ResponseEntity<EspecialistaPJCadastroResponseDTO> cadastrarPJComUsuario(
            @Valid @RequestBody EspecialistaPJComUsuarioRequestDTO request) {
        EspecialistaPJ especialistaPJ = cadastroFacadeService.cadastrarEspecialistaPJComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(especialistaPJ, true));
    }

        private EspecialistaCadastroResponseDTO toResponse(Especialista especialista, boolean acessoCriado) {
        return new EspecialistaCadastroResponseDTO(
            especialista.getIdCadastro(),
            especialista.getNome(),
            especialista.getCpf(),
            especialista.getEmail(),
            especialista.getCargo().getIdCargo(),
            especialista.getRegistroConselho(),
            acessoCriado);
        }

        private EspecialistaPJCadastroResponseDTO toResponse(EspecialistaPJ especialista, boolean acessoCriado) {
        return new EspecialistaPJCadastroResponseDTO(
            especialista.getIdCadastro(),
            especialista.getNome(),
            especialista.getCpf(),
            especialista.getEmail(),
            especialista.getCargo().getIdCargo(),
            especialista.getRegistroConselho(),
            especialista.getCnpj(),
            especialista.getRazaoSocial(),
            acessoCriado);
    }
}
