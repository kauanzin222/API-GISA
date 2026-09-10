package com.fatec.gisa.entities.paciente;

import java.util.List;

import com.fatec.gisa.enums.TipoEscola;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "Escola")
public class Escola {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEscola;
    
    private String nome;
    private String telefone;

    @Enumerated(EnumType.STRING)
    private TipoEscola tipoEscola;

    @OneToMany(mappedBy = "escola")
    private List<Paciente> alunos;
}
