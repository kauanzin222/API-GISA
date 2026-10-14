package com.fatec.gisa.controllers.paciente;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.paciente.request.EscolaCadastroRequestDTO;
import com.fatec.gisa.dtos.paciente.request.EscolaPatchRequestDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaDetalheDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.enums.TipoEscola;
import com.fatec.gisa.services.paciente.EscolaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Gestão de escolas exige GERENCIAR_ESCOLAS. O autocomplete e o cadastro rápido também são usados
// nos formulários de paciente, então aceitam ainda CADASTRAR_PACIENTE e EDITAR_PACIENTE.
@RestController
@RequestMapping("/api/escolas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_ESCOLAS')")
public class EscolaController {

    private final EscolaService escolaService;

    @GetMapping
    public ResponseEntity<Page<EscolaDetalheDTO>> listarPaginado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoEscola tipoEscola,
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(escolaService.listarPaginado(nome, tipoEscola, pageable));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_PACIENTE', 'EDITAR_PACIENTE', 'GERENCIAR_ESCOLAS')")
    @GetMapping("/sugestoes")
    public ResponseEntity<List<EscolaResponseDTO>> buscarSugestoes(@RequestParam(required = false) String nome) {
        return ResponseEntity.ok(escolaService.buscarSugestoes(nome));
    }

    @GetMapping("/{idEscola}")
    public ResponseEntity<EscolaDetalheDTO> buscarPorId(@PathVariable Long idEscola) {
        return ResponseEntity.ok(escolaService.buscarDetalhes(idEscola));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_PACIENTE', 'EDITAR_PACIENTE', 'GERENCIAR_ESCOLAS')")
    @PostMapping
    public ResponseEntity<EscolaResponseDTO> cadastrar(@Valid @RequestBody EscolaCadastroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escolaService.cadastrar(request));
    }

    @PatchMapping("/{idEscola}")
    public ResponseEntity<EscolaDetalheDTO> atualizar(
            @PathVariable Long idEscola,
            @Valid @RequestBody EscolaPatchRequestDTO request) {
        escolaService.atualizar(idEscola, request);
        return ResponseEntity.ok(escolaService.buscarDetalhes(idEscola));
    }

    @DeleteMapping("/{idEscola}")
    public ResponseEntity<Void> excluir(@PathVariable Long idEscola) {
        escolaService.excluir(idEscola);
        return ResponseEntity.noContent().build();
    }
}
