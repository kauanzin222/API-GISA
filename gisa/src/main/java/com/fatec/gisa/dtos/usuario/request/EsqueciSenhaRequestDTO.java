package com.fatec.gisa.dtos.usuario.request;

// Sem validação: a resposta é sempre 204, para não revelar quais e-mails estão cadastrados
public record EsqueciSenhaRequestDTO(
        String email) {
}
