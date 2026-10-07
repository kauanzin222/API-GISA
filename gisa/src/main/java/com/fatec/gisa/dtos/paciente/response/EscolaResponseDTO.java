package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.enums.TipoEscola;

public record EscolaResponseDTO(
        Long idEscola,
        String nome,
        TipoEscola tipoEscola,
        String telefone) {
}