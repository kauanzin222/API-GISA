package com.fatec.gisa.dtos.especialista.response;

import java.util.List;

import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;

public record EspecialistaResponseDTO(
        ProfissionalResponseDTO profissional,
        String registroConselho,
        List<EspecialidadeResponseDTO> especialidades,
        List<JornadaTrabalhoDTO> jornadas) {
}