package com.fatec.gisa.services.usuario;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioDetailService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByPessoa_Cpf(cpf)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario nao encontrado para o CPF informado."));

        List<GrantedAuthority> authorities = usuario.getPerfil() == null
                || usuario.getPerfil().getPermissoes() == null
                ? Collections.emptyList()
                : usuario.getPerfil().getPermissoes().stream()
                        .map(Permissao::getNome)
                        .map(SimpleGrantedAuthority::new)
                        .map(authority -> (GrantedAuthority) authority)
                        .toList();

        return User.withUsername(cpf)
                .password(usuario.getSenha())
                .authorities(authorities)
                .build();
    }
}
