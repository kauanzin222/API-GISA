package com.fatec.gisa.entities.usuario;

import com.fatec.gisa.entities.Pessoa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
@Table(name = "Usuario")
public class Usuario {

    @Id 
    private Long idCadastro;

    @OneToOne 
    @MapsId 
    @JoinColumn(name = "IDCadastro")
    private Pessoa pessoa;
    
    private String senha;

    @ManyToOne
    @JoinColumn(name = "IDPerfil")
    private Perfil perfil;
}
