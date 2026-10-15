package com.fatec.gisa.entities.assistentesocial;

import java.time.LocalDate;

import com.fatec.gisa.entities.paciente.Paciente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Paciente acompanhado por um assistente social. Desvincular não apaga a linha: preenche dataFim.
// Revincular depois cria uma linha nova. No máximo um vínculo ativo por par (índice único no banco).
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Paciente_AssistenteSocial")
public class VinculoAssistenteSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDVinculo")
    private Long idVinculo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "IDPaciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "IDAssistenteSocial", nullable = false)
    private AssistenteSocial assistenteSocial;

    @Column(name = "DataInicio", nullable = false)
    private LocalDate dataInicio;

    // Nulo = vínculo ativo
    @Column(name = "DataFim")
    private LocalDate dataFim;
}
