package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.enums.GrauParentesco;

public record ResponsavelVinculoResponseDTO(
        PessoaResponseDTO responsavel,
        String ocupacao,
        GrauParentesco grauParentesco) {
}
