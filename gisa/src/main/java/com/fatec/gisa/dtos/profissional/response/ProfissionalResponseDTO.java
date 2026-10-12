package com.fatec.gisa.dtos.profissional.response;

import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.dtos.usuario.response.UsuarioResumoDTO;

public record ProfissionalResponseDTO(
        PessoaResponseDTO pessoa,
        String email,
        CargoResponseDTO cargo,
        UsuarioResumoDTO usuario) {
}