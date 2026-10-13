package com.fatec.gisa.dtos.profissional.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fatec.gisa.dtos.PatchRequestDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CargoPatchRequestDTO extends PatchRequestDTO {

    private String nome;
    private String codigoCBO;

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        markProvided("nome");
    }

    @JsonSetter("codigoCBO")
    public void setCodigoCBO(String codigoCBO) {
        this.codigoCBO = codigoCBO;
        markProvided("codigoCBO");
    }
}
