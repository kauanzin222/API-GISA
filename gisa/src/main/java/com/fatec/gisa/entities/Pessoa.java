package com.fatec.gisa.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
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
public class Pessoa {
    protected Long idCadastro;
    private String cpf;
    private String rg;
    private LocalDate dataNascimento;
    private Character sexo;
    private String celular;
    private String numCNS;
}
