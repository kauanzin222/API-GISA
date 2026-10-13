package com.fatec.gisa.dtos.profissional.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CboRequestDTO(
        @NotBlank(message = "O código CBO é obrigatório")
        @Pattern(regexp = "\\d{6}", message = "O código CBO deve conter 6 dígitos numéricos")
        String codigoCBO,

        @NotBlank(message = "O título do CBO é obrigatório")
        String tituloCBO) {
}
