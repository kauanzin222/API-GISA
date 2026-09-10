package com.fatec.gisa.entities.terapia;

import java.time.LocalDateTime;
import java.util.List;

import com.fatec.gisa.entities.especialista.Especialista;
import com.fatec.gisa.enums.Modalidade;
import com.fatec.gisa.enums.StatusTerapia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "Terapia")
public class Terapia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTerapia;
    
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    private Modalidade modalidade;

    @Enumerated(EnumType.STRING)
    private StatusTerapia status;

    @OneToMany(mappedBy = "terapia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PresencaPaciente> presencaPacientes;

    @ManyToMany
       @JoinTable(
        name = "Especialista_Terapia", 
        joinColumns = @JoinColumn(name = "IDTerapia"), 
        inverseJoinColumns = @JoinColumn(name = "IDEspecialista")
    )
    private List<Especialista> especialistas;
}
