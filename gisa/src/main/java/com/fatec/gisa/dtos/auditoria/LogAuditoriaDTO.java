package com.fatec.gisa.dtos.auditoria;

import java.time.Instant;

import com.fatec.gisa.enums.AcaoAuditoria;

public record LogAuditoriaDTO(
        Long idLog,
        Instant dataHora,
        String cpfUsuario,
        // Nome da pessoa dona do CPF, quando ela está cadastrada
        String nomeUsuario,
        AcaoAuditoria acao,
        String metodo,
        String caminho,
        int statusHttp,
        // true quando a requisição foi atendida (status abaixo de 400)
        boolean sucesso,
        String ip,
        // E-mail digitado nas tentativas de login; null nos demais registros
        String emailInformado) {
}
