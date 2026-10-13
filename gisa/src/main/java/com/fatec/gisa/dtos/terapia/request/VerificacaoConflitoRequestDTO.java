package com.fatec.gisa.dtos.terapia.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Mesmo corpo do cadastro, mais a terapia que está sendo editada (que não conflita consigo mesma)
public record VerificacaoConflitoRequestDTO(
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
        LocalTime horaTermino,

        // Opcional: ao validar uma edição, a própria terapia não conta como conflito
        @Positive(message = "O ID da terapia ignorada deve ser positivo")
        Long idTerapiaIgnorada) {
}
