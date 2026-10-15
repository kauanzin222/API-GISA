package com.fatec.gisa.controllers;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.config.AuditoriaInterceptor;
import com.fatec.gisa.dtos.usuario.request.EsqueciSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.request.LoginRequestDTO;
import com.fatec.gisa.dtos.usuario.request.RedefinirSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.response.LoginResponseDTO;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.services.JwtService;
import com.fatec.gisa.services.usuario.AutenticacaoService;
import com.fatec.gisa.services.usuario.UsuarioDetailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Rotas públicas (sem token): login, "esqueci minha senha" e redefinição de senha
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AutenticacaoService autenticacaoService;
    private final UsuarioDetailService usuarioDetailService;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        // Permite auditar as tentativas que falham (ex.: senha errada) com o e-mail digitado
        request.setAttribute(AuditoriaInterceptor.ATRIBUTO_EMAIL_LOGIN, dto.email());

        Usuario usuario = autenticacaoService.buscarPorEmail(dto.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail não cadastrado."));
        String cpf = usuario.getPessoa().getCpf();
        request.setAttribute(AuditoriaInterceptor.ATRIBUTO_CPF_LOGIN, cpf);

        // O usuário do Spring Security continua sendo identificado pelo CPF
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cpf, dto.senha()));

        Optional<String> tokenRedefinicao = autenticacaoService.verificarSenhaTemporaria(usuario.getIdCadastro());
        if (tokenRedefinicao.isPresent()) {
            return LoginResponseDTO.primeiroAcesso(tokenRedefinicao.get());
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return LoginResponseDTO.login(jwtService.gerarToken(userDetails));
    }

    // Sempre 204, exista o e-mail ou não (não revela quais e-mails estão cadastrados)
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@RequestBody EsqueciSenhaRequestDTO dto) {
        autenticacaoService.esqueciSenha(dto.email());
        return ResponseEntity.noContent().build();
    }

    // Primeiro acesso e "esqueci minha senha". A resposta já traz o token de login normal
    @PostMapping("/redefinir-senha")
    public LoginResponseDTO redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO dto) {
        String cpf = autenticacaoService.redefinirSenha(dto);
        return new LoginResponseDTO(jwtService.gerarToken(usuarioDetailService.loadUserByUsername(cpf)));
    }
}
