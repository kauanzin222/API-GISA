package com.fatec.gisa.dtos.usuario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
    @NotBlank(message = "O CPF é obrigatório.")
    @Size(min = 11, max = 14, message = "O CPF deve ter entre 11 e 14 caracteres.")
    String cpf,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, max = 30, message = "A senha deve conter entre 8 e 30 caracteres.")
    String senha,

    @NotNull(message = "O ID do perfil é obrigatório.")
    Long idPerfil
) {}
