package com.fatec.gisa.services.usuario;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fatec.gisa.services.BuscaPorIds;
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
        return BuscaPorIds.buscarTodos(idsPermissoes, permissaoRepository::findAllById,
                Permissao::getIdPermissao, "Permissão(ões) não encontrada(s)");
    }

    public PermissaoResumoDTO toResumoDTO(Permissao permissao) {
        return new PermissaoResumoDTO(
                permissao.getIdPermissao(),
                permissao.getNome(),
                permissao.getDescricao(),
                permissao.getCategoria());
    }
}
