package com.fatec.gisa.dtos.paciente.response;

import java.util.List;

import com.fatec.gisa.enums.StatusPaciente;

public record PacienteResumoDTO(
        Long idCadastro,
        String nome,
        int idade,
        String cpf,
        List<ResponsavelResumoDTO> responsaveis,
        List<CidResumoDTO> cids,
        StatusPaciente statusPaciente) {
}