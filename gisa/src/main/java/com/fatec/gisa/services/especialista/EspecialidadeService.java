package com.fatec.gisa.services.especialista;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fatec.gisa.services.BuscaPorIds;
import com.fatec.gisa.dtos.especialista.request.EspecialidadePatchRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialidadeRequestDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeDetalheDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialidadeListaDTO;
import com.fatec.gisa.dtos.especialista.response.EspecialistaVinculadoDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.enums.TipoCadastro;
import com.fatec.gisa.exceptions.ResourceNotFoundException;
import com.fatec.gisa.repositories.especialista.EspecialidadeRepository;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialidadeService {

    private final EspecialidadeRepository especialidadeRepository;
    private final EspecialistaRepository especialistaRepository;

    @Transactional
    public Set<Especialidade> obterOuCriar(List<Long> especialidadesIds,
            List<EspecialidadeRequestDTO> novasEspecialidades) {
        boolean semExistentes = especialidadesIds == null || especialidadesIds.isEmpty();
        boolean semNovas = novasEspecialidades == null || novasEspecialidades.isEmpty();

        if (semExistentes && semNovas) {
            throw new IllegalArgumentException(
                    "O especialista deve possuir ao menos uma especialidade.");
        }

        // Set evita que a mesma especialidade seja vinculada duas vezes ao especialista
        Set<Especialidade> resultado = new LinkedHashSet<>();

        if (!semExistentes) {
            resultado.addAll(BuscaPorIds.buscarTodos(especialidadesIds, especialidadeRepository::findAllById,
                    Especialidade::getIdEspecialidade, "Especialidade(s) não encontrada(s)"));
        }

        if (!semNovas) {
            for (EspecialidadeRequestDTO novaDto : novasEspecialidades) {
                Especialidade especialidade = especialidadeRepository
                        .findByNomeIgnoreCase(novaDto.getNome().trim())
                        .orElseGet(() -> criar(novaDto));
                resultado.add(especialidade);
            }
        }

        if (resultado.isEmpty()) {
            throw new IllegalArgumentException(
                    "O especialista deve possuir ao menos uma especialidade.");
        }

        return resultado;
    }

    @Transactional
    public Especialidade criar(EspecialidadeRequestDTO dto) {
        String nome = dto.getNome().trim();
        if (especialidadeRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Já existe uma especialidade com esse nome.");
        }

        Especialidade especialidade = new Especialidade();
        especialidade.setNome(nome);
        especialidade.setDescricao(dto.getDescricao());
        return especialidadeRepository.save(especialidade);
    }

    // Ordenada por nome; quantidadeEspecialistas inclui PJ e cadastros inativos
    @Transactional(readOnly = true)
    public List<EspecialidadeListaDTO> listarTodas() {
        Map<Long, Long> especialistasPorEspecialidade = especialistaRepository.contarPorEspecialidade().stream()
                .collect(Collectors.toMap(linha -> (Long) linha[0], linha -> (Long) linha[1]));

        return especialidadeRepository.findAll(Sort.by("nome")).stream()
                .map(especialidade -> new EspecialidadeListaDTO(
                        especialidade.getIdEspecialidade(),
                        especialidade.getNome(),
                        especialidade.getDescricao(),
                        especialistasPorEspecialidade.getOrDefault(especialidade.getIdEspecialidade(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EspecialidadeDetalheDTO buscarDetalhes(Long idEspecialidade) {
        Especialidade especialidade = buscarEspecialidade(idEspecialidade);

        List<EspecialistaVinculadoDTO> especialistas = especialistaRepository
                .buscarPorEspecialidade(idEspecialidade).stream()
                .map(especialista -> new EspecialistaVinculadoDTO(
                        especialista.getIdCadastro(),
                        especialista.getNome(),
                        TipoCadastro.de(especialista),
                        especialista.getStatusCadastro()))
                .toList();

        return new EspecialidadeDetalheDTO(
                especialidade.getIdEspecialidade(),
                especialidade.getNome(),
                especialidade.getDescricao(),
                especialistas.size(),
                especialistas);
    }

    @Transactional
    public Especialidade atualizar(Long idEspecialidade, EspecialidadePatchRequestDTO dto) {
        Especialidade especialidade = buscarEspecialidade(idEspecialidade);

        if (dto.hasField("nome")) {
            if (dto.getNome() == null || dto.getNome().isBlank()) {
                throw new IllegalArgumentException("O nome da especialidade não pode ser vazio.");
            }
            String nome = dto.getNome().trim();
            if (especialidadeRepository.existsByNomeIgnoreCaseAndIdEspecialidadeNot(nome, idEspecialidade)) {
                throw new IllegalArgumentException("Já existe uma especialidade com esse nome.");
            }
            especialidade.setNome(nome);
        }
        if (dto.hasField("descricao")) {
            especialidade.setDescricao(dto.getDescricao());
        }

        return especialidadeRepository.save(especialidade);
    }

    // Especialidades não têm status: o DELETE exclui, e só sem especialistas vinculados (nem inativos)
    @Transactional
    public void excluir(Long idEspecialidade) {
        Especialidade especialidade = buscarEspecialidade(idEspecialidade);
        if (especialistaRepository.existePorEspecialidade(idEspecialidade)) {
            throw new IllegalArgumentException(
                    "Não é possível excluir a especialidade: há especialistas vinculados a ela.");
        }
        especialidadeRepository.delete(especialidade);
    }

    private Especialidade buscarEspecialidade(Long idEspecialidade) {
        return especialidadeRepository.findById(idEspecialidade)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialidade não encontrada com ID: " + idEspecialidade));
    }
}
