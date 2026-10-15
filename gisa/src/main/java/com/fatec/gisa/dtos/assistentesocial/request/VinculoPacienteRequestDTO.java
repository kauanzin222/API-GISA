package com.fatec.gisa.dtos.assistentesocial.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VinculoPacienteRequestDTO(
        @NotNull(message = "O ID do paciente é obrigatório.")
        @Positive(message = "O ID do paciente deve ser positivo")
        Long idPaciente) {
}
