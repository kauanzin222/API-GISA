package com.fatec.gisa.controllers.especialista;

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

import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJCadastroRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJComUsuarioRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPJPatchRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaPatchRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaCadastroResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJCadastroResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaPJResponseDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaResponseDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.AcessoConcedidoDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.EspecialistaPJ;
import com.fatec.gisa.services.cadastro.CadastroComAcesso;
import com.fatec.gisa.services.cadastro.CadastroFacadeService;
import com.fatec.gisa.services.especialista.EspecialistaService;
import com.fatec.gisa.services.especialista.EspecialistaPJService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/especialistas")
@RequiredArgsConstructor
public class EspecialistaController {

    private final CadastroFacadeService cadastroFacadeService;
    private final EspecialistaService especialistaService;
    private final EspecialistaPJService especialistaPJService;

    // Cadastro do especialista logado (assinatura de atestado e receita, "Minha Agenda"). Basta estar
    // logado: o perfil Especialista não tem VISUALIZAR_PROFISSIONAIS. O caminho literal "/me" tem
    // prioridade sobre "/{idCadastro}" no Spring MVC, então não é lido como um ID.
    @GetMapping("/me")
    public ResponseEntity<Record> buscarUsuarioLogado(Authentication authentication) {
        return ResponseEntity.ok(especialistaService.buscarDetalhesDoUsuarioLogado(authentication.getName()));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PROFISSIONAIS')")
    @GetMapping("/{idCadastro}")
    public ResponseEntity<EspecialistaResponseDTO> buscarPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.ok(especialistaService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'VISUALIZAR_PROFISSIONAIS')")
    @GetMapping("/pj/{idCadastro}")
    public ResponseEntity<EspecialistaPJResponseDTO> buscarPJPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.ok(especialistaPJService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PROFISSIONAL')")
    @PatchMapping("/{idCadastro}")
    public ResponseEntity<EspecialistaResponseDTO> atualizar(
            @PathVariable Long idCadastro,
            @Valid @RequestBody EspecialistaPatchRequestDTO request) {
        especialistaService.atualizar(idCadastro, request);
        return ResponseEntity.ok(especialistaService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'INATIVAR_PROFISSIONAL')")
    @DeleteMapping("/{idCadastro}")
    public ResponseEntity<Void> inativar(@PathVariable Long idCadastro) {
        especialistaService.inativar(idCadastro);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_USUARIOS')")
    @PostMapping("/{idCadastro}/usuario")
    public ResponseEntity<AcessoConcedidoDTO> concederAcesso(
            @PathVariable Long idCadastro,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.status(201)
                .body(cadastroFacadeService.concederAcessoEspecialista(idCadastro, request));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'EDITAR_PROFISSIONAL')")
    @PatchMapping("/pj/{idCadastro}")
    public ResponseEntity<EspecialistaPJResponseDTO> atualizarPJ(
            @PathVariable Long idCadastro,
            @Valid @RequestBody EspecialistaPJPatchRequestDTO request) {
        especialistaPJService.atualizar(idCadastro, request);
        return ResponseEntity.ok(especialistaPJService.buscarDetalhes(idCadastro));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'INATIVAR_PROFISSIONAL')")
    @DeleteMapping("/pj/{idCadastro}")
    public ResponseEntity<Void> inativarPJ(@PathVariable Long idCadastro) {
        especialistaPJService.inativar(idCadastro);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_USUARIOS')")
    @PostMapping("/pj/{idCadastro}/usuario")
    public ResponseEntity<AcessoConcedidoDTO> concederAcessoPJ(
            @PathVariable Long idCadastro,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.status(201)
                .body(cadastroFacadeService.concederAcessoEspecialistaPJ(idCadastro, request));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_ESPECIALISTA')")
    @PostMapping
    public ResponseEntity<EspecialistaCadastroResponseDTO> cadastrar(
            @Valid @RequestBody EspecialistaCadastroRequestDTO request) {
        Especialista especialista = cadastroFacadeService.cadastrarEspecialista(request);
        return ResponseEntity.status(201).body(toResponse(especialista, false, null));
    }

    // Cria o usuário já com um perfil: escolher perfil é gerenciar usuários (como o idPerfil no PATCH)
    @PreAuthorize("hasAuthority('ACESSO_TOTAL')"
            + " or (hasAuthority('CADASTRAR_ESPECIALISTA') and hasAuthority('GERENCIAR_USUARIOS'))")
    @PostMapping("/com-usuario")
    public ResponseEntity<EspecialistaCadastroResponseDTO> cadastrarComUsuario(
            @Valid @RequestBody EspecialistaComUsuarioRequestDTO request) {
        CadastroComAcesso<Especialista> resultado = cadastroFacadeService.cadastrarEspecialistaComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(resultado.cadastro(), true, resultado.emailEnviado()));
    }

    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'CADASTRAR_ESPECIALISTA_PJ')")
    @PostMapping("/pj")
    public ResponseEntity<EspecialistaPJCadastroResponseDTO> cadastrarPJ(
            @Valid @RequestBody EspecialistaPJCadastroRequestDTO request) {
        EspecialistaPJ especialistaPJ = cadastroFacadeService.cadastrarEspecialistaPJ(request);
        return ResponseEntity.status(201).body(toResponse(especialistaPJ, false, null));
    }

    // Cria o usuário já com um perfil: escolher perfil é gerenciar usuários (como o idPerfil no PATCH)
    @PreAuthorize("hasAuthority('ACESSO_TOTAL')"
            + " or (hasAuthority('CADASTRAR_ESPECIALISTA_PJ') and hasAuthority('GERENCIAR_USUARIOS'))")
    @PostMapping("/pj/com-usuario")
    public ResponseEntity<EspecialistaPJCadastroResponseDTO> cadastrarPJComUsuario(
            @Valid @RequestBody EspecialistaPJComUsuarioRequestDTO request) {
        CadastroComAcesso<EspecialistaPJ> resultado = cadastroFacadeService.cadastrarEspecialistaPJComUsuario(request);
        return ResponseEntity.status(201).body(toResponse(resultado.cadastro(), true, resultado.emailEnviado()));
    }

        private EspecialistaCadastroResponseDTO toResponse(Especialista especialista, boolean acessoCriado,
            Boolean emailEnviado) {
        return new EspecialistaCadastroResponseDTO(
            especialista.getIdCadastro(),
            especialista.getNome(),
            especialista.getCpf(),
            especialista.getEmail(),
            especialista.getCargo().getIdCargo(),
            especialista.getRegistroConselho(),
            acessoCriado,
            emailEnviado);
        }

        private EspecialistaPJCadastroResponseDTO toResponse(EspecialistaPJ especialista, boolean acessoCriado,
            Boolean emailEnviado) {
        return new EspecialistaPJCadastroResponseDTO(
            especialista.getIdCadastro(),
            especialista.getNome(),
            especialista.getCpf(),
            especialista.getEmail(),
            especialista.getCargo().getIdCargo(),
            especialista.getRegistroConselho(),
            especialista.getCnpj(),
            especialista.getRazaoSocial(),
            acessoCriado,
            emailEnviado);
    }
}
