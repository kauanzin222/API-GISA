package com.fatec.gisa.services;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    // Momento da emissão em milissegundos: o "iat" só guarda segundos, e um acesso retirado e
    // concedido de novo no mesmo segundo deixaria passar um token antigo
    private static final String CLAIM_EMISSAO = "emi";

    public String gerarToken(UserDetails userDetails) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername()) // CPF
                .claim(CLAIM_VERSAO, versaoAtual(userDetails))
                .claim(CLAIM_EMISSAO, agora)
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + expirationMs))
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
                && versaoDoToken(token) == versaoAtual(userDetails)
                && !emitidoAntesDoUsuario(token, userDetails);
    }

    // Tokens emitidos antes desta claim existir não a têm e equivalem à versão 0
    private int versaoDoToken(String token) {
        Integer versao = extrairClaims(token).get(CLAIM_VERSAO, Integer.class);
        return versao == null ? 0 : versao;
    }

    private int versaoAtual(UserDetails userDetails) {
        return userDetails instanceof UsuarioAutenticado usuario ? usuario.getVersaoToken() : 0;
    }

    // Compara em milissegundos. Tokens emitidos antes da claim "emi" existir só têm o "iat",
    // em segundos; eles são todos de usuários anteriores a esta regra (data de criação antiga)
    private boolean emitidoAntesDoUsuario(String token, UserDetails userDetails) {
        if (!(userDetails instanceof UsuarioAutenticado usuario) || usuario.getDataCriacao() == null) {
            return false;
        }
        Claims claims = extrairClaims(token);
        Long emissaoMs = claims.get(CLAIM_EMISSAO, Long.class);
        if (emissaoMs == null) {
            return claims.getIssuedAt().toInstant()
                    .isBefore(usuario.getDataCriacao().truncatedTo(ChronoUnit.SECONDS));
        }
        return Instant.ofEpochMilli(emissaoMs).isBefore(usuario.getDataCriacao().truncatedTo(ChronoUnit.MILLIS));
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
