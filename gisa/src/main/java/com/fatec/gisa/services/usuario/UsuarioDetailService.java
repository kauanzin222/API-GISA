package com.fatec.gisa.services.usuario;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;

import org.springframework.transaction.annotation.Transactional;
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
        if (usuario.getPessoa().getStatusCadastro() == StatusCadastro.INATIVO) {
            throw new UsernameNotFoundException("O cadastro deste usuário está inativo.");
        }

        List<GrantedAuthority> authorities = usuario.getPerfil() == null
                || usuario.getPerfil().getPermissoes() == null
                ? Collections.emptyList()
                : usuario.getPerfil().getPermissoes().stream()
                        .map(Permissao::getNome)
                        .map(SimpleGrantedAuthority::new)
                        .map(authority -> (GrantedAuthority) authority)
                        .toList();

        return new UsuarioAutenticado(cpf, usuario.getSenha(), authorities, usuario.getVersaoToken());
    }
}
