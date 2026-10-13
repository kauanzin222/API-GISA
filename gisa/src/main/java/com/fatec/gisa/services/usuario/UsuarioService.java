package com.fatec.gisa.services.usuario;

import java.util.Comparator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.usuario.request.AlterarSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.request.RedefinirSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.PermissaoResumoDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioLogadoDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioResumoDTO;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.repositories.usuario.PerfilRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;
import com.fatec.gisa.services.profissional.ProfissionalMapper;



@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfissionalMapper profissionalMapper;

    // Constructor Padrão
    public UsuarioService(UsuarioRepository usuarioRepository,
                          PerfilRepository perfilRepository,
                          PasswordEncoder passwordEncoder,
                          ProfissionalMapper profissionalMapper) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.profissionalMapper = profissionalMapper;
    }

    // cpf vem do token do usuário logado
    @Transactional(readOnly = true)
    public UsuarioLogadoDTO buscarUsuarioLogado(String cpf) {
        Usuario usuario = usuarioRepository.findByPessoa_Cpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        UsuarioResumoDTO acesso = profissionalMapper.toUsuarioResumoDTO(usuario);
        Pessoa pessoa = usuario.getPessoa();
        return new UsuarioLogadoDTO(
                pessoa.getIdCadastro(),
                pessoa.getNome(),
                pessoa.getCpf(),
                TipoCadastro.de(pessoa),
                acesso.perfil(),
                acesso.permissoes().stream()
                        .sorted(Comparator.comparing(PermissaoResumoDTO::idPermissao))
                        .toList());
    }

    // Método chamado dentro do fluxo de cadastro de Profissional
    @Transactional
    public Usuario criarUsuarioParaPessoa(Pessoa pessoa, UsuarioRequestDTO dto) {
        return criarUsuario(pessoa, dto);
    }

    @Transactional
    public Usuario criarAcessoParaPessoa(Pessoa pessoa, UsuarioRequestDTO dto) {
        if (pessoa.getStatusCadastro() == StatusCadastro.INATIVO) {
            throw new IllegalArgumentException(
                    "Não é possível conceder acesso a um cadastro inativo.");
        }
        return criarUsuario(pessoa, dto);
    }

    private Usuario criarUsuario(Pessoa pessoa, UsuarioRequestDTO dto) {
        if (usuarioRepository.existsById(pessoa.getIdCadastro())) {
            throw new IllegalArgumentException("Esta pessoa já possui um usuário cadastrado.");
        }

        Perfil perfil = perfilRepository.findById(dto.idPerfil())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado com ID: " + dto.idPerfil()));

        Usuario usuario = new Usuario();
        usuario.setPessoa(pessoa); // O @MapsId vai atribuir o mesmo IDCadastro de Pessoa
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setPerfil(perfil);

        return usuarioRepository.save(usuario);
    }

    // cpf vem do token do usuário logado
    @Transactional
    public void alterarMinhaSenha(String cpf, AlterarSenhaRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByPessoa_Cpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        // 400, e não 401: um 401 faria o front entender que a sessão expirou
        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("A senha atual está incorreta.");
        }
        if (!dto.novaSenha().equals(dto.confirmacaoNovaSenha())) {
            throw new IllegalArgumentException("A confirmação não corresponde à nova senha.");
        }
        if (dto.novaSenha().equals(dto.senhaAtual())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        trocarSenha(usuario, dto.novaSenha());
    }

    @Transactional
    public void redefinirSenha(Long idCadastro, RedefinirSenhaRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Este cadastro não possui usuário de acesso. ID: " + idCadastro));
        trocarSenha(usuario, dto.novaSenha());
    }

    // Grava a nova senha e invalida os tokens emitidos antes da troca
    private void trocarSenha(Usuario usuario, String novaSenha) {
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setVersaoToken(usuario.getVersaoToken() + 1);
        usuarioRepository.save(usuario);
    }
}