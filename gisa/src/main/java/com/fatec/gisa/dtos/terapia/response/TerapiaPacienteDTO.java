package com.fatec.gisa.dtos.terapia.response;

import com.fatec.gisa.enums.StatusPresenca;

public record TerapiaPacienteDTO(
        Long idCadastro,
        String nome,
        StatusPresenca presenca,
        String justificativa) {
}
