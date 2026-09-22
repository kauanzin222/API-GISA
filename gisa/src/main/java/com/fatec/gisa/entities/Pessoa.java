package com.fatec.gisa.entities;

import java.time.LocalDate;
import java.util.List;

import com.fatec.gisa.entities.usuario.Usuario;
import com.fatec.gisa.enums.EstadoCivil;
import com.fatec.gisa.enums.StatusCadastro;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "Pessoa")
@Inheritance(strategy = InheritanceType.JOINED)
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long idCadastro;
    
    private String nome;
    private String cpf;
    private String rg;
    private LocalDate dataNascimento;
    private Character sexo;
    private String celular;
    private String numCNS;

    @Enumerated(EnumType.STRING)
    private EstadoCivil estadoCivil;

    @Enumerated(EnumType.STRING)
    private StatusCadastro statusCadastro = StatusCadastro.ATIVO;

    @OneToOne(mappedBy = "pessoa", cascade = CascadeType.ALL)
    private Usuario usuario;

    @ManyToMany(mappedBy = "moradores")
    private List<Endereco> enderecos;
}
