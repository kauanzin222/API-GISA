package com.fatec.gisa.dtos.paciente.request;

import com.fatec.gisa.dtos.PessoaPatchRequestDTO;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResponsavelPatchRequestDTO extends PessoaPatchRequestDTO {

    private String ocupacao;

    @JsonSetter("ocupacao")
    public void setOcupacao(String ocupacao) {
        this.ocupacao = ocupacao;
        markProvided("ocupacao");
    }
}
