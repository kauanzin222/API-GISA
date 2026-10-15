package com.fatec.gisa.exceptions;

// O e-mail não pôde ser enviado e a operação foi desfeita (respondido com 503)
public class EnvioEmailException extends RuntimeException {

    public EnvioEmailException(String message) {
        super(message);
    }
}
