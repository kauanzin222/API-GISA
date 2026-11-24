package com.fatec.gisa.exceptions;

import java.util.List;

// Responsável que passa a residir com o paciente e ainda mora com outros pacientes dele, sem dizer se continua
// lá (409). Só a quantidade de outros pacientes vai na resposta (LGPD)
public class ResidenciaEmOutrosPacientesException extends ConflitoException {

    private final List<ResidenciaEmOutrosPacientes> residencias;

    public ResidenciaEmOutrosPacientesException(List<ResidenciaEmOutrosPacientes> residencias) {
        super("Responsável que passa a residir com o paciente também reside com outros pacientes. "
                + "Informe se ele continua morando com eles.");
        this.residencias = List.copyOf(residencias);
    }

    public List<ResidenciaEmOutrosPacientes> getResidencias() {
        return residencias;
    }

    public record ResidenciaEmOutrosPacientes(Long idResponsavel, int outrosPacientes) {
    }
}
