package com.fatec.gisa.controllers.profissional;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalCadastroRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalComUsuarioRequestDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalCadastroResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResumoDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.AcessoConcedidoDTO;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.services.cadastro.CadastroComAcesso;
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

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PROFISSIONAIS', 'VISUALIZAR_TERAPIAS')")
    @GetMapping
    public ResponseEntity<Page<ProfissionalResumoDTO>> listarPaginado(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String nome,
            // Aceita mais de um: ?tipo=ESPECIALISTA&tipo=ESPECIALISTA_PJ
            @RequestParam(required = false) List<TipoCadastro> tipo,
            @RequestParam(required = false) StatusCadastro statusCadastro,
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProfissionalResumoDTO> pagina = profissionalService.listarPaginado(
                FiltroPessoaDTO.de(cpf, nome), tipo, statusCadastro, pageable);
        return ResponseEntity.ok(pagina);
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PROFISSIONAIS')")
    @GetMapping("/{idCadastro}")
    public ResponseEntity<ProfissionalResponseDTO> buscarPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.ok(profissionalService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PROFISSIONAL')")
    @PatchMapping("/{idCadastro}")
    public ResponseEntity<ProfissionalResponseDTO> atualizar(
            @PathVariable Long idCadastro,
            @Valid @RequestBody ProfissionalPatchRequestDTO request) {
        profissionalService.atualizar(idCadastro, request);
        return ResponseEntity.ok(profissionalService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'INATIVAR_PROFISSIONAL')")
    @DeleteMapping("/{idCadastro}")
    public ResponseEntity<Void> inativar(@PathVariable Long idCadastro) {
        profissionalService.inativar(idCadastro);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_USUARIOS')")
    @PostMapping("/{idCadastro}/usuario")
    public ResponseEntity<AcessoConcedidoDTO> concederAcesso(
            @PathVariable Long idCadastro,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.status(201)
                .body(cadastroFacadeService.concederAcessoProfissional(idCadastro, request));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_PROFISSIONAL')")
    @PostMapping
    public ResponseEntity<ProfissionalCadastroResponseDTO> cadastrar(
            @Valid @RequestBody ProfissionalCadastroRequestDTO request) {
        Profissional profissional = cadastroFacadeService.cadastrarProfissional(request);
        return ResponseEntity.status(201).body(toResponse(profissional, false, null));
    }

    // Cria o usuário já com um perfil: escolher perfil é gerenciar usuários (como o idPerfil no PATCH)
    @PreAuthorize("hasAuthority('ACESSO_TOTAL')"
            + " or (hasAuthority('CADASTRAR_PROFISSIONAL') and hasAuthority('GERENCIAR_USUARIOS'))")
    @PostMapping("/com-usuario")
    public ResponseEntity<ProfissionalCadastroResponseDTO> cadastrarComUsuario(
            @Valid @RequestBody ProfissionalComUsuarioRequestDTO request) {
        CadastroComAcesso<Profissional> resultado = cadastroFacadeService.cadastrarProfissionalComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(resultado.cadastro(), true, resultado.emailEnviado()));
    }

    private ProfissionalCadastroResponseDTO toResponse(Profissional profissional, boolean acessoCriado,
            Boolean emailEnviado) {
        return new ProfissionalCadastroResponseDTO(
                profissional.getIdCadastro(),
                profissional.getNome(),
                profissional.getCpf(),
                profissional.getEmail(),
                profissional.getCargo().getIdCargo(),
                acessoCriado,
                emailEnviado);
    }
}
