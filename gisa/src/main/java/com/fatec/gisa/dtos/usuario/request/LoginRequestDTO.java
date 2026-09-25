package com.fatec.gisa.dtos.usuario.request;

public record LoginRequestDTO(
        String cpf,
        String senha) {
}
