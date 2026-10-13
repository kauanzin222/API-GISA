package com.fatec.gisa.repositories.terapia;

import java.time.LocalDateTime;

// Terapia já existente que ocupa o horário de um paciente ou especialista
public record OcupacaoHorario(
        Long idCadastro,
        String nome,
        Long idTerapia,
        LocalDateTime inicio,
        LocalDateTime fim) {
}
