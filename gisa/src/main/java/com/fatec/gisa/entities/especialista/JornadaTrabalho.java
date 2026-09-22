package com.fatec.gisa.entities.especialista;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "JornadaTrabalho")
public class JornadaTrabalho {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDJornada")
    private Long idJornada;

    @Column(name = "DiaSemana")
    private Integer diaSemana;

    @Column(name = "HoraInicio")
    private LocalTime horaInicio;

    @Column(name = "HoraTermino")
    private LocalTime horaTermino;

    @ManyToOne
    @JoinColumn(name = "IDEspecialista")
    private Especialista especialista;
}
