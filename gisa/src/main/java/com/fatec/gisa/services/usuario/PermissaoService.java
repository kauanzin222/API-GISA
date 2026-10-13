package com.fatec.gisa.services.usuario;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.dtos.usuario.response.PermissaoResumoDTO;
import com.fatec.gisa.entities.usuario.Permissao;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.usuario.PermissaoRepository;

import lombok.RequiredArgsConstructor;

// Permissões representam funcionalidades do sistema e são definidas por migration,
// por isso a API só as consulta.
@Service
@RequiredArgsConstructor
public class PermissaoService {

    private final PermissaoRepository permissaoRepository;

    @Transactional(readOnly = true)
    public List<PermissaoResumoDTO> listarTodas() {
        return permissaoRepository.findAll(Sort.by("nome")).stream()
                .map(this::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissaoResumoDTO buscarPorId(Long idPermissao) {
        return permissaoRepository.findById(idPermissao)
                .map(this::toResumoDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permissão não encontrada com ID: " + idPermissao));
    }

    // IDs repetidos são unificados; IDs inexistentes são listados no erro
    @Transactional(readOnly = true)
    public List<Permissao> buscarPorIds(Collection<Long> idsPermissoes) {
        Set<Long> idsUnicos = idsPermissoes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Permissao> encontradas = permissaoRepository.findAllById(idsUnicos);

        Set<Long> idsNaoEncontrados = new LinkedHashSet<>(idsUnicos);
        encontradas.forEach(permissao -> idsNaoEncontrados.remove(permissao.getIdPermissao()));
        if (!idsNaoEncontrados.isEmpty()) {
            throw new IllegalArgumentException("Permissão(ões) não encontrada(s): "
                    + idsNaoEncontrados.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
        return encontradas;
    }

    public PermissaoResumoDTO toResumoDTO(Permissao permissao) {
        return new PermissaoResumoDTO(
                permissao.getIdPermissao(),
                permissao.getNome(),
                permissao.getDescricao());
    }
}
