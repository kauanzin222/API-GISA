package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.dtos.PessoaResponseDTO;

public record ResponsavelDetalheDTO(
        PessoaResponseDTO pessoa,
        String ocupacao) {
}
