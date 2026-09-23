package com.fatec.gisa.entities;

import java.util.List;

import jakarta.persistence.Column;
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
@EqualsAndHashCode(exclude = "moradores")
@Table(name = "Endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDEndereco")
    private Long idEndereco;
    
    @Column(name = "CEP")
    private String cep;

    @Column(name = "Rua")
    private String rua;

    @Column(name = "Cidade")
    private String cidade;

    @Column(name = "Bairro")
    private String bairro;

    @Column(name = "Estado")
    private String estado;

    @Column(name = "Numero")
    private String numero;

    @Column(name = "Complemento")
    private String complemento;

    @ManyToMany 
    @JoinTable(
        name = "Endereco_Pessoa",
        joinColumns = @JoinColumn(name = "IDEndereco"),
        inverseJoinColumns = @JoinColumn(name = "IDCadastro")
    )
    private List<Pessoa> moradores;
}
