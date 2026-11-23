package com.fatec.gisa.exceptions;

// Regra de negócio violada por um campo do request (400 com o campo em "fields").
// É uma IllegalArgumentException para quem já trata esse tipo
public class CampoInvalidoException extends IllegalArgumentException {

    private final String campo;

    public CampoInvalidoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
