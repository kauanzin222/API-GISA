package com.fatec.gisa.enums;

public enum AcaoAuditoria {
    LOGIN,
    // Consulta a dados sensíveis (detalhes de paciente ou responsável, a própria auditoria)
    CONSULTA,
    CRIACAO,
    ALTERACAO,
    EXCLUSAO
}
