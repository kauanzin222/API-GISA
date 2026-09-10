package com.fatec.gisa.entities.paciente;

import com.fatec.gisa.entities.paciente.pk.VinculoResponsavelPK;
import com.fatec.gisa.enums.GrauParentesco;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Responsavel_Paciente")
public class VinculoResponsavel {

    @EmbeddedId
    private VinculoResponsavelPK id = new VinculoResponsavelPK();

    @ManyToOne
    @MapsId("idPaciente")
    @JoinColumn(name = "IDPaciente")
    private Paciente paciente;

    @ManyToOne
    @MapsId("idResponsavel")
    @JoinColumn(name = "IDResponsavel")
    private Responsavel responsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "GrauParentesco")
    private GrauParentesco grauParentesco;
}
