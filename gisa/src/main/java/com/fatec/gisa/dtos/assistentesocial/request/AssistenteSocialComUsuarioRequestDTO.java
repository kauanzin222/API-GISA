package com.fatec.gisa.dtos.assistentesocial.request;

import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AssistenteSocialComUsuarioRequestDTO(
        @NotNull(message = "Os dados do assistente social são obrigatórios.")
        @Valid
        AssistenteSocialCadastroRequestDTO assistenteSocial,

        @NotNull(message = "Os dados do usuário são obrigatórios.")
        @Valid
        UsuarioRequestDTO usuario) {
}
