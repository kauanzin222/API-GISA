package com.fatec.gisa.dtos.especialista.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record EspecialistaPJCadastroResponseDTO(
        Long idCadastro,
        String nome,
        String cpf,
        String email,
        Long idCargo,
        String registroConselho,
        String cnpj,
        String razaoSocial,
        boolean acessoCriado,
        // Só nos cadastros com acesso: false quando o e-mail com a senha temporária não foi enviado
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean emailEnviado) {
}
