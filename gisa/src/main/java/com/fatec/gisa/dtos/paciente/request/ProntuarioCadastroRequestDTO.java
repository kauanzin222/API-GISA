package com.fatec.gisa.dtos.paciente.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProntuarioCadastroRequestDTO {

    private String alergias;
    private String comorbidade;
    private String mobilidade;
}