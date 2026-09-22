package com.fatec.gisa.entities.profissional;

import com.fatec.gisa.entities.Pessoa;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "Profissional")
@PrimaryKeyJoinColumn(name = "IDProfissional", referencedColumnName = "IDCadastro")
public class Profissional extends Pessoa {

    private String email;

    @ManyToOne
    @JoinColumn(name = "IDCargo")
    private Cargo cargo;
}
