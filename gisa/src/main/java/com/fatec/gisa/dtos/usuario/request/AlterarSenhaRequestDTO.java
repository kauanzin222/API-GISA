package com.fatec.gisa.dtos.usuario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequestDTO(
        @NotBlank(message = "A senha atual é obrigatória.")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 30, message = "A senha deve conter entre 8 e 30 caracteres.")
        String novaSenha,

        @NotBlank(message = "A confirmação da nova senha é obrigatória.")
        String confirmacaoNovaSenha) {
}
