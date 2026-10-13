package com.fatec.gisa.exceptions;

// Operação incompatível com o estado atual do registro (respondida com 409).
// Ex.: alterar o perfil de acesso de um cadastro que não tem usuário.
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}
