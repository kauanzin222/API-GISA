package com.fatec.gisa.dtos.especialista.request;

import java.util.List;

import com.fatec.gisa.dtos.especialista.JornadaTrabalhoDTO;
import com.fatec.gisa.dtos.profissional.request.ProfissionalPatchRequestDTO;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EspecialistaPatchRequestDTO extends ProfissionalPatchRequestDTO {

    private List<Long> especialidadesIds;
    private List<@Valid EspecialidadeRequestDTO> novasEspecialidades;
    private List<@Valid JornadaTrabalhoDTO> jornadas;

    @JsonSetter("especialidadesIds")
    public void setEspecialidadesIds(List<Long> especialidadesIds) {
        this.especialidadesIds = especialidadesIds;
        markProvided("especialidadesIds");
    }

    @JsonSetter("novasEspecialidades")
    public void setNovasEspecialidades(List<@Valid EspecialidadeRequestDTO> novasEspecialidades) {
        this.novasEspecialidades = novasEspecialidades;
        markProvided("novasEspecialidades");
    }

    @JsonSetter("jornadas")
    public void setJornadas(List<@Valid JornadaTrabalhoDTO> jornadas) {
        this.jornadas = jornadas;
        markProvided("jornadas");
    }
}
