package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.enums.StatusCadastro;

// Item da busca de pacientes (GET /api/pacientes) e das listas de pacientes do assistente social.
// O CPF completo não sai aqui (LGPD): só os dígitos do meio, ex.: "***.456.789-**"
public record PacienteBuscaDTO(
        Long idCadastro,
        String nome,
        String cpfMascarado,
        StatusCadastro statusCadastro) {
}
