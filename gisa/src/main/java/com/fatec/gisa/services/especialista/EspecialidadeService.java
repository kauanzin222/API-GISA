package com.fatec.gisa.services.especialista;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.request.EspecialidadeRequestDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.repositories.especialista.EspecialidadeRepository;

import jakarta.transaction.Transactional;
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

        Set<Especialidade> resultado = new HashSet<>();

        if (!semExistentes) {
            List<Especialidade> encontradas = especialidadeRepository.findAllById(especialidadesIds);
            if (encontradas.size() != especialidadesIds.size()) {
                throw new IllegalArgumentException("Uma ou mais especialidades nao foram encontradas.");
            }
            resultado.addAll(encontradas);
        }

        if (!semNovas) {
            for (EspecialidadeRequestDTO novaDto : novasEspecialidades) {
                Especialidade especialidade = especialidadeRepository.findByNome(novaDto.getNome())
                        .orElseGet(() -> criar(novaDto));
                resultado.add(especialidade);
            }
        }

        return resultado;
    }

    private Especialidade criar(EspecialidadeRequestDTO dto) {
        Especialidade especialidade = new Especialidade();
        especialidade.setNome(dto.getNome());
        especialidade.setDescricao(dto.getDescricao());
        return especialidadeRepository.save(especialidade);
    }
}
