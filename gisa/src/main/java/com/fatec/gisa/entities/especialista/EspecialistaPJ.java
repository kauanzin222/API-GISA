package com.fatec.gisa.entities.especialista;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "EspecialistaPJ")
@PrimaryKeyJoinColumn(name = "IDEspecialistaPJ", referencedColumnName = "IDEspecialista")
public class EspecialistaPJ extends Especialista {

    @Column(name = "CNPJ")
    private String cnpj;

    @Column(name = "RazaoSocial")
    private String razaoSocial;

    @Column(name = "NomeFantasia")
    private String nomeFantasia;

    @Column(name = "InscricaoEstadual")
    private String inscricaoEstadual;
}
