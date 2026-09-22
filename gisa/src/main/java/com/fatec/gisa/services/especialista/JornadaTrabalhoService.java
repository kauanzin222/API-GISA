package com.fatec.gisa.services.especialista;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.JornadaTrabalho;

@Service
public class JornadaTrabalhoService {

    public List<JornadaTrabalho> mapear(List<JornadaTrabalhoDTO> jornadasDTO,
            Especialista especialista) {
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
