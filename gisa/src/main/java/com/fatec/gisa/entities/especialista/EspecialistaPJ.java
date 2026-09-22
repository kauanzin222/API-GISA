package com.fatec.gisa.entities.especialista;

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
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String inscricaoEstadual;
}
