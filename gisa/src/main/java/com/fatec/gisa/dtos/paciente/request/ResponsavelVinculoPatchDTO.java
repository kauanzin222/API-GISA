package com.fatec.gisa.dtos.paciente.request;

import java.util.List;

import com.fatec.gisa.enums.GrauParentesco;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResponsavelVinculoPatchDTO(
        @NotNull @Positive Long idResponsavel,
        @NotNull GrauParentesco grauParentesco,
        // Só por compatibilidade (usado quando indicesEnderecoResidencia não vem): true = mora em todos os
        // endereços do paciente, false = não reside, null mantém a residência atual (em vínculo novo, false)
        Boolean resideComPaciente,
        // Posições, na lista "enderecos" do mesmo request, dos endereços do paciente em que o responsável mora.
        // [] = não reside. Exige "enderecos" no request
        List<Integer> indicesEnderecoResidencia) {

    public ResponsavelVinculoPatchDTO(Long idResponsavel, GrauParentesco grauParentesco, Boolean resideComPaciente) {
        this(idResponsavel, grauParentesco, resideComPaciente, null);
    }
}
