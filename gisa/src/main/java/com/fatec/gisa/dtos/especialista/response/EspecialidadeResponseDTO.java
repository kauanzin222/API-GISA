package com.fatec.gisa.dtos.especialista.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadeResponseDTO {

    private Long idEspecialidade;
    private String nome;
    private String descricao;
}
