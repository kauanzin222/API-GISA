package com.fatec.gisa.dtos.assistentesocial.response;

import java.util.List;

import com.fatec.gisa.dtos.paciente.response.PacienteBuscaDTO;
import com.fatec.gisa.dtos.profissional.response.ProfissionalResponseDTO;

// Detalhes do assistente social, com os pacientes de vínculo ativo (ordenados pelo nome)
public record AssistenteSocialResponseDTO(
        ProfissionalResponseDTO profissional,
        String registroConselho,
        List<PacienteBuscaDTO> pacientes) {
}
