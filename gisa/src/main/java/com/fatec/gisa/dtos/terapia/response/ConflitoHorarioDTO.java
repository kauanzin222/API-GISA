package com.fatec.gisa.dtos.terapia.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

// Pessoa que já tem outra terapia no horário pedido
public record ConflitoHorarioDTO(
        // "PACIENTE" ou "ESPECIALISTA"
        String tipo,
        Long idCadastro,
        String nome,
        Long idTerapia,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaTermino) {
}
