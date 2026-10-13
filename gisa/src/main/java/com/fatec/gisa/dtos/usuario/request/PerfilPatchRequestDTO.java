package com.fatec.gisa.dtos.usuario.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fatec.gisa.dtos.PatchRequestDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PerfilPatchRequestDTO extends PatchRequestDTO {

    private String nome;

    // Substitui todas as permissões do perfil
    private List<Long> permissoesIds;

    @JsonSetter("nome")
    public void setNome(String nome) {
        this.nome = nome;
        markProvided("nome");
    }

    @JsonSetter("permissoesIds")
    public void setPermissoesIds(List<Long> permissoesIds) {
        this.permissoesIds = permissoesIds;
        markProvided("permissoesIds");
    }
}
