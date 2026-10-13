package com.fatec.gisa.services.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.repositories.usuario.PerfilRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;



@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor Padrão
    public UsuarioService(UsuarioRepository usuarioRepository, 
                          PerfilRepository perfilRepository, 
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Método chamado dentro do fluxo de cadastro de Profissional
    @Transactional
    public Usuario criarUsuarioParaPessoa(Pessoa pessoa, UsuarioRequestDTO dto) {
        return criarUsuario(pessoa, dto, false);
    }

    @Transactional
    public Usuario criarAcessoParaPessoa(Pessoa pessoa, UsuarioRequestDTO dto) {
        if (pessoa.getStatusCadastro() == StatusCadastro.INATIVO) {
            throw new IllegalArgumentException(
                    "Não é possível conceder acesso a um cadastro inativo.");
        }
        return criarUsuario(pessoa, dto, true);
    }

    private Usuario criarUsuario(Pessoa pessoa, UsuarioRequestDTO dto, boolean erroComoRequisicaoInvalida) {
        if (usuarioRepository.existsById(pessoa.getIdCadastro())) {
            if (erroComoRequisicaoInvalida) {
                throw new IllegalArgumentException("Esta pessoa já possui um usuário cadastrado.");
            }
            throw new IllegalStateException("Esta pessoa já possui um usuário cadastrado.");
        }

        Perfil perfil = perfilRepository.findById(dto.idPerfil())
                .orElseThrow(() -> new IllegalArgumentException("Perfil de acesso não encontrado."));

        Usuario usuario = new Usuario();
        usuario.setPessoa(pessoa); // O @MapsId vai atribuir o mesmo IDCadastro de Pessoa
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setPerfil(perfil);

        return usuarioRepository.save(usuario);
    }
}