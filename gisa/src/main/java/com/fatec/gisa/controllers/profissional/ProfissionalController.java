package com.fatec.gisa.controllers.profissional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalComUsuarioRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalCadastroResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioResumoDTO;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;
import com.fatec.gisa.services.profissional.ProfissionalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

    private final ProfissionalService profissionalService;
    private final CadastroFacadeService cadastroFacadeService;

    @GetMapping
    public ResponseEntity<Page<ProfissionalResumoDTO>> listarPaginado(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String nome,
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProfissionalResumoDTO> pagina = profissionalService.listarPaginado(
                FiltroPessoaDTO.de(cpf, nome), pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{idCadastro}")
    public ResponseEntity<ProfissionalResponseDTO> buscarPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.of(profissionalService.buscarDetalhes(idCadastro));
    }

    @PatchMapping("/{idCadastro}")
    public ResponseEntity<ProfissionalResponseDTO> atualizar(
            @PathVariable Long idCadastro,
            @Valid @RequestBody ProfissionalPatchRequestDTO request) {
        profissionalService.atualizar(idCadastro, request);
        return ResponseEntity.of(profissionalService.buscarDetalhes(idCadastro));
    }

    @DeleteMapping("/{idCadastro}")
    public ResponseEntity<Void> inativar(@PathVariable Long idCadastro) {
        profissionalService.inativar(idCadastro);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idCadastro}/usuario")
    public ResponseEntity<UsuarioResumoDTO> concederAcesso(
            @PathVariable Long idCadastro,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.status(201)
                .body(cadastroFacadeService.concederAcessoProfissional(idCadastro, request));
    }

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
