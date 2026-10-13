package com.fatec.gisa.dtos.terapia.request;

import com.fatec.gisa.enums.StatusPresenca;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresencaRequestDTO(
        // PENDENTE desfaz uma marcação feita por engano
        @NotNull(message = "Informe a presença")
        StatusPresenca presenca,

        // Obrigatória para FALTA_JUSTIFICADA; ignorada nos demais casos
        @Size(max = 255, message = "A justificativa deve ter no máximo 255 caracteres")
        String justificativa) {
}
