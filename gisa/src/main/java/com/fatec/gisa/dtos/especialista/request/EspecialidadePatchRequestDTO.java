package com.fatec.gisa.dtos.especialista.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fatec.gisa.dtos.PatchRequestDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EspecialidadePatchRequestDTO extends PatchRequestDTO {

    private String nome;
    private String descricao;

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        markProvided("nome");
    }

    @JsonSetter("descricao")
    public void setDescricao(String descricao) {
        this.descricao = descricao;
        markProvided("descricao");
    }
}
