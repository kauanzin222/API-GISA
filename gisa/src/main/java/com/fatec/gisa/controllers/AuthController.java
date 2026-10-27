package com.fatec.gisa.controllers;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
import com.fatec.gisa.exceptions.MuitasTentativasException;
import com.fatec.gisa.services.JwtService;
import com.fatec.gisa.services.usuario.AutenticacaoService;
import com.fatec.gisa.services.usuario.LimiteTentativas;
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
    private final LimiteTentativas limiteTentativas;

    // Por e-mail, na janela de gisa.auth.janela-tentativas-minutos (padrão 15 minutos)
    @Value("${gisa.auth.max-falhas-login:5}")
    private int maxFalhasLogin;

    @Value("${gisa.auth.max-pedidos-esqueci-senha:3}")
    private int maxPedidosEsqueciSenha;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        // Permite auditar as tentativas que falham (ex.: senha errada) com o e-mail digitado
        request.setAttribute(AuditoriaInterceptor.ATRIBUTO_EMAIL_LOGIN, dto.email());

        // Bloqueia o e-mail, mesmo com a senha certa, depois de muitas falhas seguidas
        String chave = "login:" + normalizar(dto.email());
        if (limiteTentativas.excedeu(chave, maxFalhasLogin)) {
            throw new MuitasTentativasException(
                    "Muitas tentativas de login sem sucesso. Aguarde alguns minutos e tente novamente.");
        }

        Authentication authentication;
        Usuario usuario;
        try {
            usuario = autenticacaoService.buscarPorEmail(dto.email())
                    .orElseThrow(() -> new BadCredentialsException("E-mail não cadastrado."));
            String cpf = usuario.getPessoa().getCpf();
            request.setAttribute(AuditoriaInterceptor.ATRIBUTO_CPF_LOGIN, cpf);

            // O usuário do Spring Security continua sendo identificado pelo CPF
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cpf, dto.senha()));
        } catch (AuthenticationException falha) {
            limiteTentativas.registrar(chave);
            throw falha;
        }
        limiteTentativas.limpar(chave);

        Optional<String> tokenRedefinicao = autenticacaoService.verificarSenhaTemporaria(usuario.getIdCadastro());
        if (tokenRedefinicao.isPresent()) {
            return LoginResponseDTO.primeiroAcesso(tokenRedefinicao.get());
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return LoginResponseDTO.login(jwtService.gerarToken(userDetails));
    }

    // Sempre 204, exista o e-mail ou não (não revela quais e-mails estão cadastrados). Acima do
    // limite de pedidos para o mesmo e-mail, responde igual, mas não envia mais nada
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@RequestBody EsqueciSenhaRequestDTO dto) {
        String chave = "esqueci-senha:" + normalizar(dto.email());
        if (!limiteTentativas.excedeu(chave, maxPedidosEsqueciSenha)) {
            limiteTentativas.registrar(chave);
            autenticacaoService.esqueciSenha(dto.email());
        }
        return ResponseEntity.noContent().build();
    }

    // Primeiro acesso e "esqueci minha senha". A resposta já traz o token de login normal
    @PostMapping("/redefinir-senha")
    public LoginResponseDTO redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO dto) {
        String cpf = autenticacaoService.redefinirSenha(dto);
        return new LoginResponseDTO(jwtService.gerarToken(usuarioDetailService.loadUserByUsername(cpf)));
    }

    // O e-mail é comparado sem diferenciar maiúsculas (ver UsuarioRepository.buscarPorEmail)
    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
