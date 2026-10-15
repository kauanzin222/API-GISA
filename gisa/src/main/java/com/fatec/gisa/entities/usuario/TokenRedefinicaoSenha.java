package com.fatec.gisa.entities.usuario;

import java.time.Instant;

import com.fatec.gisa.enums.OrigemTokenRedefinicao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Token de uso único para cadastrar uma senha nova (primeiro acesso e "esqueci minha senha").
// Só o hash é guardado; o token em si vai para o usuário (na resposta do login ou no link do e-mail).
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TokenRedefinicaoSenha")
public class TokenRedefinicaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDToken")
    private Long idToken;

    // O usuário é apagado ao retirar o acesso; o banco apaga os tokens junto (ON DELETE CASCADE)
    @Column(name = "IDUsuario", nullable = false)
    private Long idUsuario;

    // SHA-256 do token, em hexadecimal
    @Column(name = "TokenHash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Origem", nullable = false)
    private OrigemTokenRedefinicao origem;

    @Column(name = "ExpiraEm", nullable = false)
    private Instant expiraEm;

    // Preenchido quando o token é usado ou invalidado por um token mais novo
    @Column(name = "UsadoEm")
    private Instant usadoEm;

    @Column(name = "CriadoEm", nullable = false, updatable = false)
    private Instant criadoEm;
}
