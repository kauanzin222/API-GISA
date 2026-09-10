package com.fatec.gisa.entities.paciente;

import java.time.LocalDate;
import java.util.List;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.enums.StatusPaciente;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "Paciente")
@PrimaryKeyJoinColumn(name = "IDCadastro")
public class Paciente extends Pessoa {

    @Enumerated(EnumType.STRING)
    private StatusPaciente statusPaciente;

    private LocalDate dataCadastro;
    private Boolean convenio;

    @ManyToOne
    private Escola escola;

    @ManyToMany
    @JoinTable(
        name = "Paciente_CID", 
        joinColumns = @JoinColumn(name = "IDPaciente"), 
        inverseJoinColumns = @JoinColumn(name = "CodigoCID")
    )
    private List<Cid> cids;

    @OneToOne(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Prontuario prontuario;
}
