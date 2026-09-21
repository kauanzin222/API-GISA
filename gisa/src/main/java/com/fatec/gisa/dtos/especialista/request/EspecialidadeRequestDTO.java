package com.fatec.gisa.dtos.especialista.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadeRequestDTO {

    @NotBlank(message = "O nome da especialidade é obrigatório")
    private String nome;

    private String descricao;
}
