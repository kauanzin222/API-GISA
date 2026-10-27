package com.fatec.gisa;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.entities.especialista.JornadaTrabalho;

// Sem jornada o especialista não pode ser agendado. Para os testes que não tratam de jornada,
// ele atende o dia inteiro, todos os dias (gravada junto com o especialista, por cascata)
public final class JornadasDeTeste {

    private JornadasDeTeste() {
    }

    public static List<JornadaTrabalho> diaInteiroTodosOsDias(Especialista especialista) {
        List<JornadaTrabalho> jornadas = new ArrayList<>();
        for (int diaSemana = 1; diaSemana <= 7; diaSemana++) {
            jornadas.add(new JornadaTrabalho(null, diaSemana, LocalTime.MIDNIGHT, LocalTime.of(23, 59), especialista));
        }
        return jornadas;
    }
}
