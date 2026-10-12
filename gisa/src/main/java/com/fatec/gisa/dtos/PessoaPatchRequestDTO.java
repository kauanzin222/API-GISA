package com.fatec.gisa.dtos;

import java.util.List;

import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusCadastro;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PessoaPatchRequestDTO extends PatchRequestDTO {

    private String nome;
    private String celular;
    private String numCNS;
    private EstadoCivil estadoCivil;
    private StatusCadastro statusCadastro;
    private List<@Valid EnderecoDTO> enderecos;

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        markProvided("nome");
    }

    @JsonSetter("celular")
    public void setCelular(String celular) {
        this.celular = celular;
        markProvided("celular");
    }

    @JsonSetter("numCNS")
    public void setNumCNS(String numCNS) {
        this.numCNS = numCNS;
        markProvided("numCNS");
    }

    @JsonSetter("estadoCivil")
    public void setEstadoCivil(EstadoCivil estadoCivil) {
        this.estadoCivil = estadoCivil;
        markProvided("estadoCivil");
    }

    @JsonSetter("statusCadastro")
    public void setStatusCadastro(StatusCadastro statusCadastro) {
        this.statusCadastro = statusCadastro;
        markProvided("statusCadastro");
    }

    @JsonSetter("enderecos")
    public void setEnderecos(List<@Valid EnderecoDTO> enderecos) {
        this.enderecos = enderecos;
        markProvided("enderecos");
    }
}
