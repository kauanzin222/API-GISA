package com.fatec.gisa.dtos.paciente.response;

import java.util.List;

import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.enums.GrauParentesco;

public record ResponsavelVinculoResponseDTO(
        PessoaResponseDTO responsavel,
        String ocupacao,
        GrauParentesco grauParentesco,
        // Derivado: compartilha ao menos um endereço com o paciente
        boolean resideComPaciente,
        // IDs (ordenados) dos endereços do paciente em que o responsável mora
        List<Long> idsEnderecoResidencia) {
}
