package com.fatec.gisa.entities.paciente;

import java.util.List;

import com.fatec.gisa.entities.Pessoa;

import jakarta.persistence.Entity;
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
@Table(name = "Responsavel")
@PrimaryKeyJoinColumn(name = "IDResponsavel", referencedColumnName = "IDCadastro")
public class Responsavel extends Pessoa {
    private String ocupacao;

    @OneToMany(mappedBy = "responsavel")
    private List<VinculoResponsavel> dependentes;
}
