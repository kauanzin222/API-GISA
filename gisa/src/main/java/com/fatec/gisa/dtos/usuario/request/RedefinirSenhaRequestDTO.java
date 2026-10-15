package com.fatec.gisa.dtos.usuario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Primeiro acesso (token da resposta do login) e "esqueci minha senha" (token do link do e-mail)
public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "O token de redefinição é obrigatório.")
        String token,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 30, message = "A senha deve conter entre 8 e 30 caracteres.")
        String novaSenha,

        @NotBlank(message = "A confirmação da nova senha é obrigatória.")
        String confirmacaoNovaSenha) {
}
