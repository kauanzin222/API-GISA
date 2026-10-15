package com.fatec.gisa.dtos.usuario.request;

import jakarta.validation.constraints.NotNull;

// Conceder acesso: a senha não é enviada. O back gera uma senha temporária e a envia ao e-mail do cadastro
public record UsuarioRequestDTO(
    @NotNull(message = "O ID do perfil é obrigatório.")
    Long idPerfil
) {}
