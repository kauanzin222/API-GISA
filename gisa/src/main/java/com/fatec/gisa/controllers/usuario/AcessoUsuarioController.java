package com.fatec.gisa.controllers.usuario;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.usuario.request.AlterarPerfilRequestDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioDetalheDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioListaDTO;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.services.usuario.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Administração dos usuários de acesso. As rotas do próprio usuário (/me, /minha-senha) e a
// redefinição de senha ficam em UsuarioController. O ID é o idCadastro da pessoa dona do usuário.
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_USUARIOS')")
public class AcessoUsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<Page<UsuarioListaDTO>> listar(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long idPerfil,
            // Aceita mais de um: ?tipoCadastro=ESPECIALISTA&tipoCadastro=ESPECIALISTA_PJ
            @RequestParam(required = false) List<TipoCadastro> tipoCadastro,
            @RequestParam(required = false) StatusCadastro statusCadastro,
            @PageableDefault(page = 0, size = 9, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(
                FiltroPessoaDTO.de(cpf, nome), idPerfil, tipoCadastro, statusCadastro, pageable));
    }

    @GetMapping("/{idCadastro}")
    public ResponseEntity<UsuarioDetalheDTO> buscarPorId(@PathVariable Long idCadastro) {
        return ResponseEntity.ok(usuarioService.buscarDetalhes(idCadastro));
    }

    @PatchMapping("/{idCadastro}/perfil")
    public ResponseEntity<UsuarioDetalheDTO> alterarPerfil(
            @PathVariable Long idCadastro,
            @Valid @RequestBody AlterarPerfilRequestDTO request) {
        return ResponseEntity.ok(usuarioService.alterarPerfil(idCadastro, request.idPerfil()));
    }

    // Apaga só o usuário; o cadastro continua e o acesso pode ser concedido de novo
    @DeleteMapping("/{idCadastro}")
    public ResponseEntity<Void> retirarAcesso(@PathVariable Long idCadastro, Authentication authentication) {
        usuarioService.retirarAcesso(idCadastro, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
