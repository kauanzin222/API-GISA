package com.fatec.gisa.entities.paciente;

import java.time.LocalDate;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.enums.StatusPaciente;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
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
@Table(name = "Paciente")
@PrimaryKeyJoinColumn(name = "IDCadastro")
public class Paciente extends Pessoa {

    @Enumerated(EnumType.STRING)
    private StatusPaciente statusPaciente;
    
    private LocalDate dataCadastro;
    private Boolean convenio;
}
