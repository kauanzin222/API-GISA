package com.fatec.gisa.enums;

public enum AcaoAuditoria {
    // Só as tentativas de login que falharam
    LOGIN,
    // Consulta aos detalhes de um paciente (dados de saúde); nenhum outro GET é registrado
    CONSULTA,
    CRIACAO,
    ALTERACAO,
    EXCLUSAO
}
