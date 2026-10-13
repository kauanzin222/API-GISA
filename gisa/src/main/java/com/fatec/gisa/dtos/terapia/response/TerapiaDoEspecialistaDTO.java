package com.fatec.gisa.dtos.terapia.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatec.gisa.enums.Modalidade;
import com.fatec.gisa.enums.StatusTerapia;

// Listagem das terapias do especialista logado: os demais especialistas
// aparecem só pelo nome, já que o próprio especialista é implícito
public record TerapiaDoEspecialistaDTO(
        Long idTerapia,
        String titulo,
        Modalidade modalidade,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaTermino,
        StatusTerapia status,
        List<TerapiaPacienteDTO> pacientes,
        List<String> outrosEspecialistas) {
}
