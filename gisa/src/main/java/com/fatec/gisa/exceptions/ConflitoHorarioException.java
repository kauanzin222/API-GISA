package com.fatec.gisa.exceptions;

import java.util.List;

import com.fatec.gisa.dtos.terapia.response.ConflitoHorarioDTO;

// Terapia que não pode ser gravada: alguém já ocupado no horário ou especialista fora da jornada.
// Respondida com 409, com a mensagem ("Conflito de horário: ...") e a lista de conflitos no corpo
public class ConflitoHorarioException extends ConflitoException {

    private final transient List<ConflitoHorarioDTO> conflitos;

    public ConflitoHorarioException(String message, List<ConflitoHorarioDTO> conflitos) {
        super(message);
        this.conflitos = List.copyOf(conflitos);
    }

    public List<ConflitoHorarioDTO> getConflitos() {
        return conflitos;
    }
}
