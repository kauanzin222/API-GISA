package com.fatec.gisa.services.usuario;

import java.time.Instant;

// Dados para o e-mail de boas-vindas. A senha em texto só existe em memória até o envio
public record SenhaTemporariaGerada(String nome, String email, String senha, Instant expiraEm) {
}
