package com.fatec.gisa.dtos.profissional.response;

import com.fatec.gisa.dtos.PessoaResponseDTO;

public record ProfissionalResponseDTO(
        PessoaResponseDTO pessoa,
        String email,
        CargoResponseDTO cargo) {
}