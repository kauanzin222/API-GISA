package com.fatec.gisa.exceptions;

// Limite de tentativas atingido (respondido com 429). Ex.: muitas senhas erradas no login
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException(String message) {
        super(message);
    }
}
