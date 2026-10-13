package com.fatec.gisa.dtos.terapia.response;

import java.util.List;

public record VerificacaoConflitoDTO(
        boolean possuiConflito,
        List<ConflitoHorarioDTO> conflitos) {
}
