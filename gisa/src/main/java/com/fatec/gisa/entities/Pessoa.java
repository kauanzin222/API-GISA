package com.fatec.gisa.entities;

import java.time.LocalDate;

import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusCadastro;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long idCadastro;
    private String cpf;
    private String rg;
    private LocalDate dataNascimento;
    private Character sexo;
    private String celular;
    private String numCNS;
    private EstadoCivil estadoCivil;
    private StatusCadastro statusCadastro;

    @OneToOne(mappedBy = "pessoa", cascade = CascadeType.ALL)
    private Usuario usuario;
}
