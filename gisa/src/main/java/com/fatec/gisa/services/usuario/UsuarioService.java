package com.fatec.gisa.services.usuario;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.FiltroPessoaDTO;
import com.fatec.gisa.dtos.profissional.response.CargoResumoDTO;
import com.fatec.gisa.dtos.usuario.request.AlterarSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.request.RedefinirSenhaRequestDTO;
import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;
import com.fatec.gisa.dtos.usuario.response.PermissaoResumoDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioDetalheDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioListaDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioLogadoDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioResumoDTO;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.profissional.Profissional;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.repositories.PessoaRepository;
import com.fatec.gisa.repositories.usuario.PerfilRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;
import com.fatec.gisa.services.profissional.ProfissionalMapper;



@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfissionalMapper profissionalMapper;

    // Constructor Padrão
    public UsuarioService(UsuarioRepository usuarioRepository,
                          PessoaRepository pessoaRepository,
                          PerfilRepository perfilRepository,
                          PasswordEncoder passwordEncoder,
                          ProfissionalMapper profissionalMapper) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
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

    // ---------- Administração de acessos (GET/PATCH/DELETE /api/usuarios) ----------

    // tipos e statusCadastro vazios/nulos não restringem; idPerfil nulo = qualquer perfil
    @Transactional(readOnly = true)
    public Page<UsuarioListaDTO> listar(FiltroPessoaDTO filtro, Long idPerfil, List<TipoCadastro> tipos,
            StatusCadastro statusCadastro, Pageable pageable) {
        Set<TipoCadastro> tiposFiltrados = tipos == null || tipos.isEmpty()
                ? EnumSet.allOf(TipoCadastro.class)
                : EnumSet.copyOf(tipos);
        Set<StatusCadastro> status = statusCadastro == null
                ? EnumSet.allOf(StatusCadastro.class)
                : EnumSet.of(statusCadastro);

        // O PostgreSQL não infere o tipo de "? is null": sem perfil, vai o ID 0 (nenhum)
        return pessoaRepository.buscarComUsuario(filtro.cpf(), filtro.nome(), idPerfil == null ? 0L : idPerfil,
                        status,
                        tiposFiltrados.contains(TipoCadastro.PROFISSIONAL),
                        tiposFiltrados.contains(TipoCadastro.ESPECIALISTA),
                        tiposFiltrados.contains(TipoCadastro.ESPECIALISTA_PJ),
                        tiposFiltrados.contains(TipoCadastro.PESSOA),
                        pageable)
                .map(pessoa -> new UsuarioListaDTO(
                        pessoa.getIdCadastro(),
                        pessoa.getNome(),
                        pessoa.getCpf(),
                        TipoCadastro.de(pessoa),
                        profissionalMapper.toUsuarioResumoDTO(pessoa.getUsuario()).perfil(),
                        pessoa.getStatusCadastro()));
    }

    @Transactional(readOnly = true)
    public UsuarioDetalheDTO buscarDetalhes(Long idCadastro) {
        return toDetalheDTO(buscarUsuario(idCadastro));
    }

    @Transactional
    public UsuarioDetalheDTO alterarPerfil(Long idCadastro, Long idPerfil) {
        Usuario usuario = buscarUsuario(idCadastro);
        alterarPerfil(usuario, idPerfil);
        return toDetalheDTO(usuario);
    }

    // Usado também pelo PATCH de profissional/especialista com idPerfil
    @Transactional
    public void alterarPerfil(Usuario usuario, Long idPerfil) {
        // ID enviado no corpo: inexistente torna o pedido inválido (400), não a rota (404)
        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado com ID: " + idPerfil));
        boolean novoPerfilTemAcessoTotal = perfil.getPermissoes().stream()
                .anyMatch(permissao -> PermissaoUsuarioLogado.ACESSO_TOTAL.equals(permissao.getNome()));
        if (!novoPerfilTemAcessoTotal) {
            garantirOutroAdministrador(usuario);
        }
        usuario.setPerfil(perfil);
    }

    // Apaga só o usuário (login, senha e perfil); o cadastro da pessoa continua como está.
    // Os tokens dele deixam de valer na hora: o filtro JWT não encontra mais o usuário, e se o
    // acesso for concedido de novo, a data de criação recusa os tokens antigos
    @Transactional
    public void retirarAcesso(Long idCadastro, String cpfLogado) {
        Usuario usuario = buscarUsuario(idCadastro);
        Pessoa pessoa = usuario.getPessoa();
        if (pessoa.getCpf().equals(cpfLogado)) {
            throw new IllegalArgumentException("Você não pode retirar o seu próprio acesso.");
        }
        garantirOutroAdministrador(usuario);

        // Pessoa tem cascade para o usuário: sem desfazer o vínculo, o usuário seria salvo de novo
        pessoa.setUsuario(null);
        usuarioRepository.delete(usuario);
    }

    // Recusa a mudança se o usuário for o último administrador ativo. Chamado antes de alterar,
    // porque a consulta trava os administradores e enxerga o estado atual
    private void garantirOutroAdministrador(Usuario usuario) {
        List<Usuario> administradores = usuarioRepository.travarComPermissao(
                PermissaoUsuarioLogado.ACESSO_TOTAL, StatusCadastro.ATIVO);
        boolean usuarioEhAdministrador = administradores.stream()
                .anyMatch(administrador -> administrador.getIdCadastro().equals(usuario.getIdCadastro()));
        if (usuarioEhAdministrador && administradores.size() == 1) {
            throw new IllegalArgumentException("O sistema precisa de ao menos um usuário ativo com acesso total.");
        }
    }

    private Usuario buscarUsuario(Long idCadastro) {
        Pessoa pessoa = pessoaRepository.findById(idCadastro)
                .orElseThrow(() -> new ResourceNotFoundException("Cadastro não encontrado com ID: " + idCadastro));
        if (pessoa.getUsuario() == null) {
            throw new ResourceNotFoundException("Este cadastro não possui usuário de acesso. ID: " + idCadastro);
        }
        return pessoa.getUsuario();
    }

    private UsuarioDetalheDTO toDetalheDTO(Usuario usuario) {
        Pessoa pessoa = usuario.getPessoa();
        // A herança é JOINED: a pessoa pode vir como proxy da classe base
        Profissional profissional = Hibernate.unproxy(pessoa) instanceof Profissional encontrado ? encontrado : null;
        UsuarioResumoDTO acesso = profissionalMapper.toUsuarioResumoDTO(usuario);
        return new UsuarioDetalheDTO(
                pessoa.getIdCadastro(),
                pessoa.getNome(),
                pessoa.getCpf(),
                profissional == null ? null : profissional.getEmail(),
                TipoCadastro.de(pessoa),
                pessoa.getStatusCadastro(),
                profissional == null || profissional.getCargo() == null
                        ? null
                        : new CargoResumoDTO(profissional.getCargo().getIdCargo(), profissional.getCargo().getNome()),
                acesso.perfil(),
                acesso.permissoes().stream()
                        .sorted(Comparator.comparing(PermissaoResumoDTO::idPermissao))
                        .toList());
    }
}
