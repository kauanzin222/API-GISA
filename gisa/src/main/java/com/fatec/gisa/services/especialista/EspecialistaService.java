package com.fatec.gisa.services.especialista;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialidadeRequestDTO;
import com.fatec.gisa.dtos.especialista.request.EspecialistaCadastroRequestDTO;
import com.fatec.gisa.entities.especialista.Especialidade;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.JornadaTrabalho;
import com.fatec.gisa.entities.profissional.Cargo;
import com.fatec.gisa.repositories.especialista.EspecialidadeRepository;
import com.fatec.gisa.repositories.especialista.EspecialistaRepository;
import com.fatec.gisa.repositories.profissional.CargoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EspecialistaService {

    private final EspecialistaRepository especialistaRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final CargoRepository cargoRepository;

    @Transactional
    public Especialista cadastrar(EspecialistaCadastroRequestDTO dto) {
        Especialista especialista = new Especialista();

        especialista.setNome(dto.getNome());
        especialista.setCpf(dto.getCpf());
        especialista.setRg(dto.getRg());
        especialista.setDataNascimento(dto.getDataNascimento());
        especialista.setSexo(dto.getSexo());
        especialista.setCelular(dto.getCelular());
        especialista.setEmail(dto.getEmail());
        especialista.setNumCNS(dto.getNumCNS());
        especialista.setEstadoCivil(dto.getEstadoCivil());

        Cargo cargo = cargoRepository.findById(dto.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado"));

        especialista.setCargo(cargo);
        especialista.setRegistroConselho(dto.getRegistroConselho());

        Set<Especialidade> especialidades = processarEspecialidades(
                dto.getEspecialidadesIds(),
                dto.getNovasEspecialidades()
        );

        especialista.setEspecialidades(new ArrayList<>(especialidades));
        especialista.setJornadaTrabalho(mapearJornadas(dto.getJornadas(), especialista));

        return especialistaRepository.save(especialista);
    }

    private Set<Especialidade> processarEspecialidades(List<Long> especialidadesIds,
                                                      List<EspecialidadeRequestDTO> novasEspecialidades) {
        boolean semExistentes = especialidadesIds == null || especialidadesIds.isEmpty();
        boolean semNovas = novasEspecialidades == null || novasEspecialidades.isEmpty();

        if (semExistentes && semNovas) {
            throw new IllegalArgumentException("O especialista deve possuir ao menos uma especialidade.");
        }

        Set<Especialidade> resultado = new HashSet<>();

        if (!semExistentes) {
            List<Especialidade> encontradas = especialidadeRepository.findAllById(especialidadesIds);
            if (encontradas.size() != especialidadesIds.size()) {
                throw new IllegalArgumentException("Especialidade inválida.");
            }
            resultado.addAll(encontradas);
        }

        if (!semNovas) {
            for (EspecialidadeRequestDTO novaDto : novasEspecialidades) {
                Especialidade especialidade = especialidadeRepository.findByNome(novaDto.getNome())
                        .orElseGet(() -> {
                            Especialidade nova = new Especialidade();
                            nova.setNome(novaDto.getNome());
                            nova.setDescricao(novaDto.getDescricao());
                            return especialidadeRepository.save(nova);
                        });
                resultado.add(especialidade);
            }
        }

        return resultado;
    }

    private List<JornadaTrabalho> mapearJornadas(List<JornadaTrabalhoDTO> jornadasDTO, Especialista especialista) {
        if (jornadasDTO == null || jornadasDTO.isEmpty()) {
            return new ArrayList<>();
        }

        return jornadasDTO.stream().map(dto -> {
            JornadaTrabalho jornada = new JornadaTrabalho();
            jornada.setDiaSemana(dto.getDiaSemana());
            jornada.setHoraInicio(dto.getHoraInicio());
            jornada.setHoraTermino(dto.getHoraTermino());
            jornada.setEspecialista(especialista);
            return jornada;
        }).collect(Collectors.toList());
    }
}
