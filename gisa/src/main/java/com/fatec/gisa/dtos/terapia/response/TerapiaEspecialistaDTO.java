package com.fatec.gisa.dtos.terapia.response;

import java.util.List;

public record TerapiaEspecialistaDTO(
        Long idCadastro,
        String nome,
        List<String> especialidades) {
}
