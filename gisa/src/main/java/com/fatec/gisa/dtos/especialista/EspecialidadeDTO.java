package com.fatec.gisa.dtos.especialista;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadeDTO {

    private Integer idEspecialidade;
    private String nome;
    private String descricao;
}
