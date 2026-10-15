package com.fatec.gisa.exceptions;

// Login recusado com uma mensagem própria (respondido com 401). Ex.: senha temporária expirada.
// Senha errada e cadastro inativo continuam com a mensagem genérica "Credenciais inválidas."
public class NaoAutorizadoException extends RuntimeException {

    public NaoAutorizadoException(String message) {
        super(message);
    }
}
