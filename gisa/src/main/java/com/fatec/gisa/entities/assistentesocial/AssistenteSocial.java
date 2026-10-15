package com.fatec.gisa.entities.assistentesocial;

import com.fatec.gisa.entities.profissional.Profissional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Especialização de Profissional (mesmo padrão de Especialista). Os pacientes acompanhados ficam
// em VinculoAssistenteSocial
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "AssistenteSocial")
@PrimaryKeyJoinColumn(name = "IDAssistenteSocial", referencedColumnName = "IDProfissional")
public class AssistenteSocial extends Profissional {

    // Número do CRESS
    @Column(name = "RegistroConselho")
    private String registroConselho;
}
