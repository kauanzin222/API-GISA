package com.fatec.gisa.dtos.especialista.request;

import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EspecialistaComUsuarioRequestDTO(
        @NotNull(message = "Os dados do especialista são obrigatórios.")
        @Valid
        EspecialistaCadastroRequestDTO especialista,

        @NotNull(message = "Os dados do usuário são obrigatórios.")
        @Valid
        UsuarioRequestDTO usuario) {
}
