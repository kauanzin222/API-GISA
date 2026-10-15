package com.fatec.gisa.dtos.terapia.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

// Motivo pelo qual um paciente ou especialista não pode estar na terapia pedida
public record ConflitoHorarioDTO(
        // "PACIENTE" ou "ESPECIALISTA"
        String tipo,
        // "HORARIO_OCUPADO" ou "FORA_DA_JORNADA"
        String motivo,
        Long idCadastro,
        String nome,
        // HORARIO_OCUPADO: a terapia existente que ocupa o horário. FORA_DA_JORNADA: null, e data e
        // horários são os da terapia pedida
        Long idTerapia,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaTermino,
        // Só em FORA_DA_JORNADA: as jornadas do especialista naquele dia da semana ([] se não atende)
        @JsonInclude(JsonInclude.Include.NON_NULL) List<JornadaDoDiaDTO> jornadasDoDia) {

    public static final String PACIENTE = "PACIENTE";
    public static final String ESPECIALISTA = "ESPECIALISTA";
    public static final String HORARIO_OCUPADO = "HORARIO_OCUPADO";
    public static final String FORA_DA_JORNADA = "FORA_DA_JORNADA";

    public record JornadaDoDiaDTO(
            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaTermino) {
    }
}
