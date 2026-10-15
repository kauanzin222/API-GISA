package com.fatec.gisa.dtos.terapia.response;

import java.util.List;

import com.fatec.gisa.enums.StatusPresenca;

public record TerapiaPacienteDTO(
        Long idCadastro,
        String nome,
        StatusPresenca presenca,
        String justificativa,
        // Marcações aceitas agora pelo PATCH de presença (depende do dia da terapia e do prazo);
        // vazia nas terapias canceladas
        List<StatusPresenca> presencasPermitidas) {
}
