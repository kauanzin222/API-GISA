package com.fatec.gisa.dtos.terapia.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

// Terapia futura agendada que ficaria fora da nova jornada do especialista (PATCH de especialista)
public record TerapiaForaDaJornadaDTO(
        Long idTerapia,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaTermino) {
}
