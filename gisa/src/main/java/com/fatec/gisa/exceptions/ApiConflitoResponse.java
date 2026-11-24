package com.fatec.gisa.exceptions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fatec.gisa.dtos.terapia.response.ConflitoHorarioDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaForaDaJornadaDTO;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException.EnderecoCompartilhado;
import com.fatec.gisa.exceptions.ResidenciaEmOutrosPacientesException.ResidenciaEmOutrosPacientes;

// Corpo de erro padrão (ver ApiErrorResponse) com os detalhes de um conflito de agenda (409):
// "conflitos" nos agendamentos de terapia, "terapias" na alteração de jornada do especialista,
// "enderecosCompartilhados" na alteração de endereço usado por outros cadastros e
// "residenciasEmOutrosPacientes" no responsável que passa a residir e ainda mora com outros pacientes
public record ApiConflitoResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ConflitoHorarioDTO> conflitos,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<TerapiaForaDaJornadaDTO> terapias,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<EnderecoCompartilhado> enderecosCompartilhados,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ResidenciaEmOutrosPacientes> residenciasEmOutrosPacientes) {

    public ApiConflitoResponse(Instant timestamp, int status, String error, String message, String path,
            Map<String, String> fields, List<ConflitoHorarioDTO> conflitos, List<TerapiaForaDaJornadaDTO> terapias) {
        this(timestamp, status, error, message, path, fields, conflitos, terapias, null, null);
    }
}
