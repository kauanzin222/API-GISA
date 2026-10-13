package com.fatec.gisa.dtos.usuario.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PermissoesRequestDTO(
        @NotEmpty(message = "Informe ao menos uma permissão")
        List<@NotNull(message = "O ID da permissão não pode ser nulo") Long> permissoesIds) {
}
