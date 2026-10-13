package com.fatec.gisa.dtos.profissional.request;

import com.fatec.gisa.dtos.usuario.request.UsuarioRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProfissionalComUsuarioRequestDTO(
        @NotNull(message = "Os dados do profissional são obrigatórios.") @Valid ProfissionalCadastroRequestDTO profissional,

        @NotNull(message = "Os dados do usuário são obrigatórios.") @Valid UsuarioRequestDTO usuario) {
}
