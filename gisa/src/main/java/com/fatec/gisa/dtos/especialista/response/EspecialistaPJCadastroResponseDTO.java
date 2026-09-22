package com.fatec.gisa.dtos.especialista.response;

public record EspecialistaPJCadastroResponseDTO(
        Long idCadastro,
        String nome,
        String cpf,
        String email,
        Long idCargo,
        String registroConselho,
        String cnpj,
        String razaoSocial,
        boolean acessoCriado) {
}
