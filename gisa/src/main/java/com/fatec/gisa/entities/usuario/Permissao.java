package com.fatec.gisa.entities.usuario;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@EqualsAndHashCode(exclude = "perfis")
@Table(name = "Permissao")
public class Permissao {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDPermissao")
    private Long idPermissao;
    
    @Column(name = "Nome")
    private String nome;
    private String descricao;

    @ManyToMany(mappedBy = "permissoes")
    private List<Perfil> perfis;
}
