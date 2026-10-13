package com.fatec.gisa.services.usuario;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

// Usuário do Spring Security com a versão vigente das credenciais,
// usada para recusar tokens emitidos antes da última troca de senha
@Getter
public class UsuarioAutenticado extends User {

    private final int versaoToken;

    public UsuarioAutenticado(String cpf, String senha,
            Collection<? extends GrantedAuthority> authorities, int versaoToken) {
        super(cpf, senha, authorities);
        this.versaoToken = versaoToken;
    }
}
