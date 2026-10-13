package com.fatec.gisa.dtos.usuario.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record PerfilRequestDTO(
        @NotBlank(message = "O nome do perfil é obrigatório")
        String nome,

        // Opcional: o perfil pode ser criado sem permissões e recebê-las depois
        List<Long> permissoesIds) {
}
