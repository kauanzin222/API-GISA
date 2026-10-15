package com.fatec.gisa.exceptions;

import java.util.List;

import com.fatec.gisa.dtos.terapia.response.TerapiaForaDaJornadaDTO;

// Nova jornada do especialista que deixaria terapias futuras agendadas fora do horário.
// Respondida com 409, com a lista dessas terapias no corpo
public class TerapiasForaDaJornadaException extends ConflitoException {

    private final transient List<TerapiaForaDaJornadaDTO> terapias;

    public TerapiasForaDaJornadaException(String message, List<TerapiaForaDaJornadaDTO> terapias) {
        super(message);
        this.terapias = List.copyOf(terapias);
    }

    public List<TerapiaForaDaJornadaDTO> getTerapias() {
        return terapias;
    }
}
