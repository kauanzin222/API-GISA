package com.fatec.gisa.dtos.especialista.request;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EspecialistaPJPatchRequestDTO extends EspecialistaPatchRequestDTO {

    private String razaoSocial;
    private String nomeFantasia;
    private String inscricaoEstadual;

    @JsonSetter("razaoSocial")
    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
        markProvided("razaoSocial");
    }

    @JsonSetter("nomeFantasia")
    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
        markProvided("nomeFantasia");
    }

    @JsonSetter("inscricaoEstadual")
    public void setInscricaoEstadual(String inscricaoEstadual) {
        this.inscricaoEstadual = inscricaoEstadual;
        markProvided("inscricaoEstadual");
    }
}
