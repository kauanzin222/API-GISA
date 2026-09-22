package com.fatec.gisa.dtos.especialista.request;

import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EspecialistaPJComUsuarioRequestDTO(
        @NotNull(message = "Os dados do especialista PJ sao obrigatorios.")
        @Valid
        EspecialistaPJCadastroRequestDTO especialistaPJ,

        @NotNull(message = "Os dados do usuario sao obrigatorios.")
        @Valid
        UsuarioRequestDTO usuario) {
}
