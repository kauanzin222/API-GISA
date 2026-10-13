package com.fatec.gisa.entities.auditoria;

import java.time.Instant;

import com.fatec.gisa.enums.AcaoAuditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "LogAuditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDLog")
    private Long idLog;

    @Column(name = "DataHora", nullable = false)
    private Instant dataHora;

    @Column(name = "CPFUsuario")
    private String cpfUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "Acao", nullable = false)
    private AcaoAuditoria acao;

    @Column(name = "Metodo", nullable = false)
    private String metodo;

    @Column(name = "Caminho", nullable = false)
    private String caminho;

    @Column(name = "StatusHttp", nullable = false)
    private int statusHttp;

    @Column(name = "IP")
    private String ip;
}
