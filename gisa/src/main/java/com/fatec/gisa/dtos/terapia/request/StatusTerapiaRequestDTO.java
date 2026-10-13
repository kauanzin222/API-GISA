package com.fatec.gisa.dtos.terapia.request;

import com.fatec.gisa.enums.StatusTerapia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusTerapiaRequestDTO(
        @NotNull(message = "Informe o status")
        StatusTerapia status,

        // Obrigatório para CANCELADA; ignorado nos demais casos
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres")
        String motivo) {
}
