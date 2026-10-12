package com.fatec.gisa.dtos.paciente.request;

import com.fatec.gisa.dtos.PatchRequestDTO;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProntuarioPatchRequestDTO extends PatchRequestDTO {

    private String alergias;
    private String comorbidade;
    private String mobilidade;

    @JsonSetter("alergias")
    public void setAlergias(String alergias) {
        this.alergias = alergias;
        markProvided("alergias");
    }

    @JsonSetter("comorbidade")
    public void setComorbidade(String comorbidade) {
        this.comorbidade = comorbidade;
        markProvided("comorbidade");
    }

    @JsonSetter("mobilidade")
    public void setMobilidade(String mobilidade) {
        this.mobilidade = mobilidade;
        markProvided("mobilidade");
    }
}
