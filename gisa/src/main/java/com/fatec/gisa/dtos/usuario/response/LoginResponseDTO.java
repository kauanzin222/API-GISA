package com.fatec.gisa.dtos.usuario.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// Campos nulos não aparecem na resposta:
// - login normal: { token, senhaTemporaria: false }
// - login com senha temporária: { senhaTemporaria: true, tokenRedefinicao }
// - troca e redefinição de senha: { token }
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDTO(
        String token,
        Boolean senhaTemporaria,
        String tokenRedefinicao) {

    public LoginResponseDTO(String token) {
        this(token, null, null);
    }

    public static LoginResponseDTO login(String token) {
        return new LoginResponseDTO(token, false, null);
    }

    public static LoginResponseDTO primeiroAcesso(String tokenRedefinicao) {
        return new LoginResponseDTO(null, true, tokenRedefinicao);
    }
}
