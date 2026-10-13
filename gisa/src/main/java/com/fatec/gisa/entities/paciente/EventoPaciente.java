package com.fatec.gisa.entities.paciente;

import java.time.Instant;

import com.fatec.gisa.entities.Pessoa;
import com.fatec.gisa.enums.TipoEventoPaciente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

// Evento da linha do tempo do paciente. A descrição nunca contém dado de saúde.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "EventoPaciente")
public class EventoPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDEvento")
    private Long idEvento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDPaciente", nullable = false)
    private Paciente paciente;

    @Column(name = "DataHora", nullable = false)
    private Instant dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "Tipo", nullable = false)
    private TipoEventoPaciente tipo;

    @Column(name = "Descricao", nullable = false)
    private String descricao;

    // Só nos eventos de terapia
    @Column(name = "IDTerapia")
    private Long idTerapia;

    // Quem fez a ação
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDUsuario")
    private Pessoa usuario;
}
