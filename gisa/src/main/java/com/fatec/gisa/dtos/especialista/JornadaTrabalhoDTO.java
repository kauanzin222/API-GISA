package com.fatec.gisa.dtos.especialista;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JornadaTrabalhoDTO {

    private Long idJornada;

    @NotNull(message = "O dia da semana é obrigatório")
    @Min(value = 1, message = "Dia da semana deve ser entre 1 (Segunda) e 7 (Domingo)")
    @Max(value = 7, message = "Dia da semana deve ser entre 1 (Segunda) e 7 (Domingo)")
    private Integer diaSemana;

    @NotNull(message = "A hora de início é obrigatória")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "A hora de término é obrigatória")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaTermino;
}
