package com.fatec.gisa.entities.terapia;

import com.fatec.gisa.entities.paciente.Paciente;
import com.fatec.gisa.entities.terapia.pk.PresencaPacientePK;
import com.fatec.gisa.enums.StatusPresenca;

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
@Table(name = "Paciente_Terapia")
public class PresencaPaciente {

    @EmbeddedId
    private PresencaPacientePK id = new PresencaPacientePK();

    @Enumerated(EnumType.STRING)
    private StatusPresenca presenca;
    
    private String justificativa;

    @ManyToOne
    @MapsId("idPaciente")
    @JoinColumn(name = "IDPaciente")
    private Paciente paciente;

    @ManyToOne
    @MapsId("idTerapia")
    @JoinColumn(name = "IDTerapia")
    private Terapia terapia;
}
