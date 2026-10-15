package com.fatec.gisa.enums;

// Por que o token de redefinição de senha foi gerado
public enum OrigemTokenRedefinicao {
    // Login com a senha temporária: o usuário precisa cadastrar uma senha nova
    PRIMEIRO_ACESSO,
    // POST /api/auth/esqueci-senha
    ESQUECI_SENHA
}
