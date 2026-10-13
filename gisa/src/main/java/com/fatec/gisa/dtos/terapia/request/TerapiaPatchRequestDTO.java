package com.fatec.gisa.dtos.terapia.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fatec.gisa.dtos.PatchRequestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Remarcar e trocar participantes: só o que for enviado muda; as listas substituem as atuais
@Getter
@NoArgsConstructor
public class TerapiaPatchRequestDTO extends PatchRequestDTO {

    private LocalDate data;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaTermino;

    private List<@NotNull(message = "O ID do paciente não pode ser nulo") Long> pacientesIds;

    private List<@NotNull(message = "O ID do especialista não pode ser nulo") Long> especialistasIds;

    @JsonSetter("data")
    public void setData(LocalDate data) {
        this.data = data;
        markProvided("data");
    }

    @JsonSetter("horaInicio")
    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
        markProvided("horaInicio");
    }

    @JsonSetter("horaTermino")
    public void setHoraTermino(LocalTime horaTermino) {
        this.horaTermino = horaTermino;
        markProvided("horaTermino");
    }

    @JsonSetter("pacientesIds")
    public void setPacientesIds(List<Long> pacientesIds) {
        this.pacientesIds = pacientesIds;
        markProvided("pacientesIds");
    }

    @JsonSetter("especialistasIds")
    public void setEspecialistasIds(List<Long> especialistasIds) {
        this.especialistasIds = especialistasIds;
        markProvided("especialistasIds");
    }
}
