package com.fatec.gisa.entities;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "Endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEndereco;
    
    private String cep;
    private String rua;
    private String cidade;
    private String bairro;
    private String estado;
    private String numero;
    private String complemento;

    @ManyToMany 
    @JoinTable(
        name = "Endereco_Pessoa",
        joinColumns = @JoinColumn(name = "IDEndereco"),
        inverseJoinColumns = @JoinColumn(name = "IDCadastro")
    )
    private List<Pessoa> moradores;
}
