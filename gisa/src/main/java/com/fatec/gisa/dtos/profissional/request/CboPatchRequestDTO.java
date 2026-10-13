package com.fatec.gisa.dtos.profissional.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fatec.gisa.dtos.PatchRequestDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

// O código CBO é a chave do registro e não pode ser alterado
@Getter
@NoArgsConstructor
public class CboPatchRequestDTO extends PatchRequestDTO {

    private String tituloCBO;

    @JsonSetter("tituloCBO")
    public void setTituloCBO(String tituloCBO) {
        this.tituloCBO = tituloCBO;
        markProvided("tituloCBO");
    }
}
