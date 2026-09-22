package com.fatec.gisa.dtos.especialista;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadeDTO {

    private Long idEspecialidade;
    private String nome;
    private String descricao;
}
