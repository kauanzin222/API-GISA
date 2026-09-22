package com.fatec.gisa.dtos.especialista.response;

public record EspecialistaCadastroResponseDTO(
        Long idCadastro,
        String nome,
        String cpf,
        String email,
        Long idCargo,
        String registroConselho,
        boolean acessoCriado) {
}
