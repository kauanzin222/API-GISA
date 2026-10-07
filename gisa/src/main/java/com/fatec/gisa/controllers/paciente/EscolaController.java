package com.fatec.gisa.controllers.paciente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.paciente.request.EscolaCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.services.paciente.EscolaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/escolas")
@RequiredArgsConstructor
public class EscolaController {

    private final EscolaService escolaService;

    @GetMapping
    public ResponseEntity<List<EscolaResponseDTO>> buscarSugestoes(@RequestParam String nome) {
        return ResponseEntity.ok(escolaService.buscarSugestoes(nome));
    }

    @PostMapping
    public ResponseEntity<EscolaResponseDTO> cadastrar(@Valid @RequestBody EscolaCadastroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escolaService.cadastrar(request));
    }
}