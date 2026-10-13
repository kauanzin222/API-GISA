package com.fatec.gisa.services.especialista;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialidadeRequestDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.repositories.especialista.EspecialidadeRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialidadeService {

    private final EspecialidadeRepository especialidadeRepository;

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
            // IDs repetidos na mesma requisição são unificados
            Set<Long> idsUnicos = especialidadesIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<Especialidade> encontradas = especialidadeRepository.findAllById(idsUnicos);

            Set<Long> idsNaoEncontrados = new LinkedHashSet<>(idsUnicos);
            encontradas.forEach(especialidade -> idsNaoEncontrados.remove(especialidade.getIdEspecialidade()));
            if (!idsNaoEncontrados.isEmpty()) {
                throw new IllegalArgumentException("Especialidade(s) não encontrada(s): "
                        + idsNaoEncontrados.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            }
            resultado.addAll(encontradas);
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

    @Transactional
    public List<Especialidade> listarTodas() {
        return especialidadeRepository.findAll();
    }
}
