package com.fatec.gisa.services;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.fatec.gisa.services.usuario.UsuarioAutenticado;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Claim com a versão das credenciais do usuário no momento da emissão
    private static final String CLAIM_VERSAO = "ver";

    public String gerarToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername()) // CPF
                .claim(CLAIM_VERSAO, versaoAtual(userDetails))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extrairCpf(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        String cpf = extrairCpf(token);
        return cpf.equals(userDetails.getUsername())
                && !tokenExpirado(token)
                && versaoDoToken(token) == versaoAtual(userDetails);
    }

    // Tokens emitidos antes desta claim existir não a têm e equivalem à versão 0
    private int versaoDoToken(String token) {
        Integer versao = extrairClaims(token).get(CLAIM_VERSAO, Integer.class);
        return versao == null ? 0 : versao;
    }

    private int versaoAtual(UserDetails userDetails) {
        return userDetails instanceof UsuarioAutenticado usuario ? usuario.getVersaoToken() : 0;
    }

    private boolean tokenExpirado(String token) {
        return extrairClaims(token).getExpiration().before(new Date());
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
