package com.fatec.gisa.entities.usuario;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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
@EqualsAndHashCode(exclude = "permissoes")
@Table(name = "Perfil")
public class Perfil {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDPerfil")
    private Long idPerfil;
    
    @Column(name = "Nome")
    private String nome;

    @ManyToMany
    @JoinTable(
        name = "Perfil_Permissao",
        joinColumns = @JoinColumn(name = "IDPerfil"),
        inverseJoinColumns = @JoinColumn(name = "IDPermissao")
    )
    private List<Permissao> permissoes;
}
