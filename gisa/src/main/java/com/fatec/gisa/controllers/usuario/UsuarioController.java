package com.fatec.gisa.controllers.usuario;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.dtos.usuario.request.AlterarSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.request.RedefinirSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.response.LoginResponseDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioLogadoDTO;
import com.fatec.gisa.services.JwtService;
import com.fatec.gisa.services.usuario.UsuarioDetailService;
import com.fatec.gisa.services.usuario.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioDetailService usuarioDetailService;
    private final JwtService jwtService;

    // O token só carrega o CPF; o front usa esta rota para saber quem está logado
    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoDTO> buscarUsuarioLogado(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.buscarUsuarioLogado(authentication.getName()));
    }

    // A troca invalida o token atual, então a resposta já traz um token novo
    @PatchMapping("/minha-senha")
    public ResponseEntity<LoginResponseDTO> alterarMinhaSenha(
            Authentication authentication,
            @Valid @RequestBody AlterarSenhaRequestDTO request) {
        String cpf = authentication.getName();
        usuarioService.alterarMinhaSenha(cpf, request);
        String token = jwtService.gerarToken(usuarioDetailService.loadUserByUsername(cpf));
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    // Rota sensível: sem esta restrição, qualquer usuário logado poderia tomar a conta de outro
    @PreAuthorize("hasAnyAuthority('ACESSO_TOTAL', 'GERENCIAR_USUARIOS')")
    @PatchMapping("/{idCadastro}/senha")
    public ResponseEntity<Void> redefinirSenha(
            @PathVariable Long idCadastro,
            @Valid @RequestBody RedefinirSenhaRequestDTO request) {
        usuarioService.redefinirSenha(idCadastro, request);
        return ResponseEntity.noContent().build();
    }
}
