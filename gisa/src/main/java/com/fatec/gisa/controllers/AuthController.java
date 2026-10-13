package com.fatec.gisa.controllers;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.gisa.config.AuditoriaInterceptor;
import com.fatec.gisa.dtos.usuario.request.LoginRequestDTO;
import com.fatec.gisa.dtos.usuario.response.LoginResponseDTO;
import com.fatec.gisa.services.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        // Permite auditar também as tentativas que falham (ex.: senha errada)
        request.setAttribute(AuditoriaInterceptor.ATRIBUTO_CPF_LOGIN, dto.cpf());

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.cpf(), dto.senha()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.gerarToken(userDetails);

        return new LoginResponseDTO(token);
    }
}
