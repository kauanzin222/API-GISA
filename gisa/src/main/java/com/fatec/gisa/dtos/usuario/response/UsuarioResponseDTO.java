package com.fatec.gisa.dtos.usuario.response;

public record UsuarioResponseDTO(
        Long idCadastro,
        String nome,
        String cpf,
        String perfil
) {}
