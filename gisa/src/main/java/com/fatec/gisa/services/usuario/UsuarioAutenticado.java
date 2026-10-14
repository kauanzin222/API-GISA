package com.fatec.gisa.services.usuario;

import java.time.Instant;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

// Usuário do Spring Security com o que é preciso para recusar tokens antigos: a versão vigente
// das credenciais (muda a cada troca de senha) e a data de criação do usuário (um acesso
// retirado e concedido de novo é outro usuário)
@Getter
public class UsuarioAutenticado extends User {

    private final int versaoToken;
    private final Instant dataCriacao;

    public UsuarioAutenticado(String cpf, String senha,
            Collection<? extends GrantedAuthority> authorities, int versaoToken, Instant dataCriacao) {
        super(cpf, senha, authorities);
        this.versaoToken = versaoToken;
        this.dataCriacao = dataCriacao;
    }
}
