package com.fatec.gisa.dtos.terapia.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// A modalidade não é enviada: mais de um paciente torna a terapia coletiva
public record TerapiaCadastroRequestDTO(
        @NotEmpty(message = "Informe ao menos um paciente")
        List<@NotNull(message = "O ID do paciente não pode ser nulo") Long> pacientesIds,

        @NotEmpty(message = "Informe ao menos um especialista")
        List<@NotNull(message = "O ID do especialista não pode ser nulo") Long> especialistasIds,

        @NotNull(message = "A data é obrigatória")
        LocalDate data,

        @NotNull(message = "A hora de início é obrigatória")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio,

        @NotNull(message = "A hora de término é obrigatória")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaTermino) {
}
