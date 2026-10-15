package com.fatec.gisa.services.usuario;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.PessoaVinculadaDTO;
import com.fatec.gisa.dtos.usuario.request.PerfilPatchRequestDTO;
import com.fatec.gisa.dtos.usuario.request.PerfilRequestDTO;
import com.fatec.gisa.dtos.usuario.response.PerfilDetalheDTO;
import com.fatec.gisa.dtos.usuario.response.PerfilListaDTO;
import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.entities.usuario.Perfil;
import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.usuario.PerfilRepository;
import com.fatec.gisa.repositories.usuario.UsuarioRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoService permissaoService;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<PerfilListaDTO> listarTodos() {
        Map<Long, Long> usuariosPorPerfil = usuarioRepository.contarPorPerfil().stream()
                .collect(Collectors.toMap(linha -> (Long) linha[0], linha -> (Long) linha[1]));

        return perfilRepository.findAll(Sort.by("nome")).stream()
                .map(perfil -> new PerfilListaDTO(
                        perfil.getIdPerfil(),
                        perfil.getNome(),
                        perfil.getPermissoes() == null ? 0 : perfil.getPermissoes().size(),
                        usuariosPorPerfil.getOrDefault(perfil.getIdPerfil(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PerfilDetalheDTO buscarDetalhes(Long idPerfil) {
        Perfil perfil = buscarPerfil(idPerfil);

        List<PessoaVinculadaDTO> usuarios = usuarioRepository
                .findByPerfil_IdPerfilOrderByPessoa_NomeAsc(idPerfil).stream()
                .map(usuario -> {
                    Pessoa pessoa = usuario.getPessoa();
                    return new PessoaVinculadaDTO(
                            pessoa.getIdCadastro(),
                            pessoa.getNome(),
                            pessoa.getCpf(),
                            pessoa.getStatusCadastro());
                })
                .toList();

        return new PerfilDetalheDTO(
                perfil.getIdPerfil(),
                perfil.getNome(),
                perfil.getPermissoes().stream()
                        .sorted(Comparator.comparing(Permissao::getNome))
                        .map(permissaoService::toResumoDTO)
                        .toList(),
                usuarios);
    }

    @Transactional
    public Perfil criar(PerfilRequestDTO dto) {
        String nome = dto.nome().trim();
        if (perfilRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Já existe um perfil com esse nome.");
        }

        Perfil perfil = new Perfil();
        perfil.setNome(nome);
        perfil.setPermissoes(dto.permissoesIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(permissaoService.buscarPorIds(dto.permissoesIds())));
        return perfilRepository.save(perfil);
    }

    @Transactional
    public Perfil atualizar(Long idPerfil, PerfilPatchRequestDTO dto) {
        Perfil perfil = buscarPerfil(idPerfil);

        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome do perfil não pode ser vazio.");
            }
            String nome = dto.getNome().trim();
            if (perfilRepository.existsByNomeIgnoreCaseAndIdPerfilNot(nome, idPerfil)) {
                throw new IllegalArgumentException("Já existe um perfil com esse nome.");
            }
            perfil.setNome(nome);
        }

        if (dto.hasField("permissoesIds")) {
            List<Permissao> permissoes = dto.getPermissoesIds() == null
                    ? List.of()
                    : permissaoService.buscarPorIds(dto.getPermissoesIds());
            garantirGestorDePerfis(perfil, permissoes);
            perfil.getPermissoes().clear();
            perfil.getPermissoes().addAll(permissoes);
        }

        return perfilRepository.save(perfil);
    }

    // Permissões que o perfil já possui são ignoradas
    @Transactional
    public Perfil adicionarPermissoes(Long idPerfil, List<Long> idsPermissoes) {
        Perfil perfil = buscarPerfil(idPerfil);
        for (Permissao permissao : permissaoService.buscarPorIds(idsPermissoes)) {
            boolean jaPossui = perfil.getPermissoes().stream()
                    .anyMatch(atual -> atual.getIdPermissao().equals(permissao.getIdPermissao()));
            if (!jaPossui) {
                perfil.getPermissoes().add(permissao);
            }
        }
        return perfilRepository.save(perfil);
    }

    @Transactional
    public Perfil removerPermissao(Long idPerfil, Long idPermissao) {
        Perfil perfil = buscarPerfil(idPerfil);
        List<Permissao> restantes = perfil.getPermissoes().stream()
                .filter(permissao -> !permissao.getIdPermissao().equals(idPermissao))
                .toList();
        if (restantes.size() == perfil.getPermissoes().size()) {
            throw new ResourceNotFoundException(
                    "O perfil não possui a permissão com ID: " + idPermissao);
        }
        garantirGestorDePerfis(perfil, restantes);
        perfil.getPermissoes().removeIf(permissao -> permissao.getIdPermissao().equals(idPermissao));
        return perfilRepository.save(perfil);
    }

    @Transactional
    public void excluir(Long idPerfil) {
        Perfil perfil = buscarPerfil(idPerfil);
        if (usuarioRepository.existsByPerfil_IdPerfil(idPerfil)) {
            throw new IllegalArgumentException(
                    "Não é possível excluir o perfil: há usuários vinculados a ele.");
        }
        // Sem usuários no perfil, a exclusão não tira ninguém da contagem; a verificação fica por garantia
        garantirGestorDePerfis(perfil, List.of());
        perfilRepository.delete(perfil);
    }

    // Se o perfil deixar de permitir gerenciar perfis, os usuários dele deixam de contar para a
    // proteção contra trancar o sistema (409 se não sobrar nenhum usuário ativo que gerencie perfis)
    private void garantirGestorDePerfis(Perfil perfil, List<Permissao> novasPermissoes) {
        Perfil depois = new Perfil(perfil.getIdPerfil(), perfil.getNome(), novasPermissoes);
        if (!UsuarioService.permiteGerenciarPerfis(depois)) {
            usuarioService.garantirGestorDePerfis(
                    gestor -> gestor.getPerfil().getIdPerfil().equals(perfil.getIdPerfil()));
        }
    }

    private Perfil buscarPerfil(Long idPerfil) {
        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil não encontrado com ID: " + idPerfil));
        if (perfil.getPermissoes() == null) {
            perfil.setPermissoes(new ArrayList<>());
        }
        return perfil;
    }
}
