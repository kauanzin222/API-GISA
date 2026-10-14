package com.fatec.gisa.dtos.paciente.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fatec.gisa.dtos.PatchRequestDTO;
import com.fatec.gisa.enums.TipoEscola;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EscolaPatchRequestDTO extends PatchRequestDTO {

    private String nome;
    private TipoEscola tipoEscola;
    private String telefone;

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        markProvided("nome");
    }

    @JsonSetter("tipoEscola")
    public void setTipoEscola(TipoEscola tipoEscola) {
        this.tipoEscola = tipoEscola;
        markProvided("tipoEscola");
    }

    @JsonSetter("telefone")
    public void setTelefone(String telefone) {
        this.telefone = telefone;
        markProvided("telefone");
    }
}
