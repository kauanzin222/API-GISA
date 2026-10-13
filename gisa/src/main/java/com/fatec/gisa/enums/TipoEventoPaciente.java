package com.fatec.gisa.enums;

// Eventos da linha do tempo do paciente (não confundir com as ações da auditoria)
public enum TipoEventoPaciente {
    // Cadastro e status
    CADASTRO,
    DADOS_ALTERADOS,
    STATUS_PACIENTE_ALTERADO,
    INATIVADO,
    REATIVADO,
    // Responsáveis
    RESPONSAVEL_VINCULADO,
    RESPONSAVEL_DESVINCULADO,
    // Terapias
    TERAPIA_AGENDADA,
    TERAPIA_ALTERADA,
    TERAPIA_CANCELADA,
    PRESENCA_REGISTRADA
}
