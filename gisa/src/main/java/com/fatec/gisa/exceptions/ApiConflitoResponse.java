package com.fatec.gisa.exceptions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fatec.gisa.dtos.terapia.response.ConflitoHorarioDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaForaDaJornadaDTO;
import com.fatec.gisa.exceptions.EnderecoCompartilhadoException.EnderecoCompartilhado;

// Corpo de erro padrão (ver ApiErrorResponse) com os detalhes de um conflito de agenda (409):
// "conflitos" nos agendamentos de terapia, "terapias" na alteração de jornada do especialista e
// "enderecosCompartilhados" na alteração de endereço usado por outros cadastros
public record ApiConflitoResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ConflitoHorarioDTO> conflitos,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<TerapiaForaDaJornadaDTO> terapias,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<EnderecoCompartilhado> enderecosCompartilhados) {

    public ApiConflitoResponse(Instant timestamp, int status, String error, String message, String path,
            Map<String, String> fields, List<ConflitoHorarioDTO> conflitos, List<TerapiaForaDaJornadaDTO> terapias) {
        this(timestamp, status, error, message, path, fields, conflitos, terapias, null);
    }
}
