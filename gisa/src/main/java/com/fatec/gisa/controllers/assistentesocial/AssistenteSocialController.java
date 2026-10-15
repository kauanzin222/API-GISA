package com.fatec.gisa.controllers.assistentesocial;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.assistentesocial.request.AssistenteSocialCadastroRequestDTO;
import com.fatec.gisa.dtos.assistentesocial.request.AssistenteSocialComUsuarioRequestDTO;
import com.fatec.gisa.dtos.assistentesocial.request.VinculoPacienteRequestDTO;
import com.fatec.gisa.dtos.assistentesocial.response.AssistenteSocialCadastroResponseDTO;
import com.fatec.gisa.dtos.assistentesocial.response.AssistenteSocialResponseDTO;
import com.fatec.gisa.dtos.paciente.response.PacienteBuscaDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.AcessoConcedidoDTO;
import com.fatec.gisa.entities.assistentesocial.AssistenteSocial;
import com.fatec.gisa.services.assistentesocial.AssistenteSocialService;
import com.fatec.gisa.services.cadastro.CadastroComAcesso;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Permissões no padrão dos demais profissionais. Os vínculos com pacientes (vincular/desvincular)
// são edição do assistente social: EDITAR_PROFISSIONAL, também quando feitos a partir da tela do paciente
@RestController
@RequestMapping("/api/assistentes-sociais")
@RequiredArgsConstructor
public class AssistenteSocialController {

    private final AssistenteSocialService assistenteSocialService;
    private final CadastroFacadeService cadastroFacadeService;

    // Painel do assistente social logado. Basta estar logado (como /api/especialistas/me): o que vale
    // é o usuário ser assistente social
    @GetMapping("/me/pacientes")
    public ResponseEntity<List<PacienteBuscaDTO>> listarMeusPacientes(Authentication authentication) {
        return ResponseEntity.ok(assistenteSocialService.listarPacientesDoUsuarioLogado(authentication.getName()));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PROFISSIONAIS')")
    @GetMapping("/{idCadastro}")
    public ResponseEntity<AssistenteSocialResponseDTO> buscarPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.ok(assistenteSocialService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_PROFISSIONAL')")
    @PostMapping
    public ResponseEntity<AssistenteSocialCadastroResponseDTO> cadastrar(
            @Valid @RequestBody AssistenteSocialCadastroRequestDTO request) {
        AssistenteSocial assistenteSocial = cadastroFacadeService.cadastrarAssistenteSocial(request);
        return ResponseEntity.status(201).body(toResponse(assistenteSocial, false, null));
    }

    // Cria o usuário já com um perfil: escolher perfil é gerenciar usuários (como o idPerfil no PATCH)
    @PreAuthorize("hasAuthority('ACESSO_TOTAL')"
            + " or (hasAuthority('CADASTRAR_PROFISSIONAL') and hasAuthority('GERENCIAR_USUARIOS'))")
    @PostMapping("/com-usuario")
    public ResponseEntity<AssistenteSocialCadastroResponseDTO> cadastrarComUsuario(
            @Valid @RequestBody AssistenteSocialComUsuarioRequestDTO request) {
        CadastroComAcesso<AssistenteSocial> resultado = cadastroFacadeService.cadastrarAssistenteSocialComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(resultado.cadastro(), true, resultado.emailEnviado()));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_USUARIOS')")
    @PostMapping("/{idCadastro}/usuario")
    public ResponseEntity<AcessoConcedidoDTO> concederAcesso(
            @PathVariable Long idCadastro,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.status(201)
                .body(cadastroFacadeService.concederAcessoAssistenteSocial(idCadastro, request));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PROFISSIONAL')")
    @PatchMapping("/{idCadastro}")
    public ResponseEntity<AssistenteSocialResponseDTO> atualizar(
            @PathVariable Long idCadastro,
            @Valid @RequestBody ProfissionalPatchRequestDTO request) {
        assistenteSocialService.atualizar(idCadastro, request);
        return ResponseEntity.ok(assistenteSocialService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'INATIVAR_PROFISSIONAL')")
    @DeleteMapping("/{idCadastro}")
    public ResponseEntity<Void> inativar(@PathVariable Long idCadastro) {
        assistenteSocialService.inativar(idCadastro);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PROFISSIONAL')")
    @PostMapping("/{idCadastro}/pacientes")
    public ResponseEntity<AssistenteSocialResponseDTO> vincularPaciente(
            @PathVariable Long idCadastro,
            @Valid @RequestBody VinculoPacienteRequestDTO request) {
        return ResponseEntity.status(201)
                .body(assistenteSocialService.vincularPaciente(idCadastro, request.idPaciente()));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PROFISSIONAL')")
    @DeleteMapping("/{idCadastro}/pacientes/{idPaciente}")
    public ResponseEntity<Void> desvincularPaciente(@PathVariable Long idCadastro, @PathVariable Long idPaciente) {
        assistenteSocialService.desvincularPaciente(idCadastro, idPaciente);
        return ResponseEntity.noContent().build();
    }

    private AssistenteSocialCadastroResponseDTO toResponse(AssistenteSocial assistenteSocial, boolean acessoCriado,
            Boolean emailEnviado) {
        return new AssistenteSocialCadastroResponseDTO(
                assistenteSocial.getIdCadastro(),
                assistenteSocial.getNome(),
                assistenteSocial.getCpf(),
                assistenteSocial.getEmail(),
                assistenteSocial.getCargo().getIdCargo(),
                assistenteSocial.getRegistroConselho(),
                acessoCriado,
                emailEnviado);
    }
}
