package com.fatec.gisa.dtos.paciente.response;

import java.time.LocalDate;

import com.fatec.gisa.enums.StatusPaciente;

public record PacienteCadastroResponseDTO(
        Long idCadastro,
        String nome,
        String cpf,
        StatusPaciente statusPaciente,
        LocalDate dataCadastro) {
}