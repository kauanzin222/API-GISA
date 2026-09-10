package com.fatec.gisa.entities.paciente;

import com.fatec.gisa.enums.TipoEscola;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Escola {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEscola;
    
    private String nome;
    private String telefone;

    @Enumerated(EnumType.STRING)
    private TipoEscola tipoEscola;
}
