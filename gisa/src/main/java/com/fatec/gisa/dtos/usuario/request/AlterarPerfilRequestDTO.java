package com.fatec.gisa.dtos.usuario.request;

import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequestDTO(
        @NotNull(message = "O ID do perfil é obrigatório.")
        Long idPerfil) {
}
