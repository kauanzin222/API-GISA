package com.fatec.gisa.dtos.paciente.request;

import com.fatec.gisa.enums.GrauParentesco;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResponsavelVinculoPatchDTO(
        @NotNull @Positive Long idResponsavel,
        @NotNull GrauParentesco grauParentesco,
        // null mantém o valor atual (em vínculo novo, false)
        Boolean resideComPaciente) {
}
