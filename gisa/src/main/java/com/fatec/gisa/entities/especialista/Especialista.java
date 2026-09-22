package com.fatec.gisa.entities.especialista;

import java.util.List;

import com.fatec.gisa.entities.profissional.Profissional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Especialista")
@PrimaryKeyJoinColumn(name = "IDEspecialista", referencedColumnName = "IDProfissional")
public class Especialista extends Profissional {
    @Column(name = "RegistroConselho")
    private String registroConselho;

    @ManyToMany
    @JoinTable(
        name = "Especialidade_Especialista", 
        joinColumns = @JoinColumn(name = "IDEspecialista"), 
        inverseJoinColumns = @JoinColumn(name = "IDEspecialidade")
    )
    private List<Especialidade> especialidades;

    @OneToMany(mappedBy = "especialista", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JornadaTrabalho> jornadaTrabalho;
}
