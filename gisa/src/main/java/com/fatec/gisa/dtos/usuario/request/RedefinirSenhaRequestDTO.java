package com.fatec.gisa.dtos.usuario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 30, message = "A senha deve conter entre 8 e 30 caracteres.")
        String novaSenha) {
}
