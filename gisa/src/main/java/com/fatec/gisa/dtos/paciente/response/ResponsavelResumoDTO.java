package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.enums.GrauParentesco;

public record ResponsavelResumoDTO(String nome, String celular, GrauParentesco grauParentesco) {
}
