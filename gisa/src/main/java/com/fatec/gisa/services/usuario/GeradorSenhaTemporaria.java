package com.fatec.gisa.services.usuario;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

// Senha temporária forte: 12 caracteres, com ao menos uma maiúscula, uma minúscula, um dígito e um
// símbolo. Sem caracteres que se confundem na leitura (0/O, 1/l/I), já que a pessoa vai digitá-la.
@Component
public class GeradorSenhaTemporaria {

    private static final String MAIUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String SIMBOLOS = "@#$%&*!?";
    private static final String TODOS = MAIUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;
    private static final int TAMANHO = 12;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(sortear(MAIUSCULAS));
        caracteres.add(sortear(MINUSCULAS));
        caracteres.add(sortear(DIGITOS));
        caracteres.add(sortear(SIMBOLOS));
        while (caracteres.size() < TAMANHO) {
            caracteres.add(sortear(TODOS));
        }
        Collections.shuffle(caracteres, aleatorio);
        StringBuilder senha = new StringBuilder();
        caracteres.forEach(senha::append);
        return senha.toString();
    }

    private char sortear(String conjunto) {
        return conjunto.charAt(aleatorio.nextInt(conjunto.length()));
    }
}
