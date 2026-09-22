package com.fatec.gisa.entities.paciente;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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
@Table(name = "Prontuario")
public class Prontuario {

    @Id 
    @Column(name = "IDPaciente")
    private Long idProntuario;

    @OneToOne
    @MapsId
    @JoinColumn(name = "IDPaciente")
    private Paciente paciente;

    private String alergias;
    private String comorbidade;
    private String mobilidade;
}
