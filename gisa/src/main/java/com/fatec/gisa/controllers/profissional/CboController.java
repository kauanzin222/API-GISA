package com.fatec.gisa.controllers.profissional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.profissional.request.CboPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.request.CboRequestDTO;
import com.fatec.gisa.dtos.profissional.response.CboDetalheDTO;
import com.fatec.gisa.dtos.profissional.response.CboResponseDTO;
import com.fatec.gisa.services.profissional.CboService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cbos")
public class CboController {

    private final CboService cboService;

    @GetMapping
    public ResponseEntity<Page<CboResponseDTO>> listarPaginado(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String titulo,
            @PageableDefault(page = 0, size = 9, sort = "codigoCBO", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(cboService.listarPaginado(codigo, titulo, pageable));
    }

    @GetMapping("/{codigoCBO}")
    public ResponseEntity<CboDetalheDTO> buscarPorCodigo(@PathVariable String codigoCBO) {
        return ResponseEntity.ok(cboService.buscarDetalhes(codigoCBO));
    }

    @PostMapping
    public ResponseEntity<CboResponseDTO> criar(@Valid @RequestBody CboRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cboService.criar(request));
    }

    @PatchMapping("/{codigoCBO}")
    public ResponseEntity<CboResponseDTO> atualizar(
            @PathVariable String codigoCBO,
            @Valid @RequestBody CboPatchRequestDTO request) {
        return ResponseEntity.ok(cboService.atualizar(codigoCBO, request));
    }

    @DeleteMapping("/{codigoCBO}")
    public ResponseEntity<Void> excluir(@PathVariable String codigoCBO) {
        cboService.excluir(codigoCBO);
        return ResponseEntity.noContent().build();
    }
}
