package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.enums.StatusCadastro;

public record ResponsavelListaDTO(
        Long idCadastro,
        String nome,
        String cpf,
        String celular,
        String ocupacao,
        StatusCadastro statusCadastro) {
}
