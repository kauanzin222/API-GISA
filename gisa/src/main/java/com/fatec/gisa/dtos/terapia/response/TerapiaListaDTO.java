package com.fatec.gisa.dtos.terapia.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatec.gisa.enums.Modalidade;
import com.fatec.gisa.enums.StatusTerapia;

// Listagem geral (administração) e detalhes de uma terapia
public record TerapiaListaDTO(
        Long idTerapia,
        // Nome do paciente na individual; "Terapia coletiva" na coletiva
        String titulo,
        Modalidade modalidade,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaTermino,
        StatusTerapia status,
        // Todos os pacientes justificaram a falta: o horário dos especialistas está livre
        boolean horarioLiberado,
        List<TerapiaPacienteDTO> pacientes,
        List<TerapiaEspecialistaDTO> especialistas,
        // Preenchido só nas terapias canceladas
        String motivoCancelamento) {
}
