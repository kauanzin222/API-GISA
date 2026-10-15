package com.fatec.gisa.dtos.paciente.response;

import java.time.LocalDate;
import java.util.List;

import com.fatec.gisa.dtos.PessoaResponseDTO;
import com.fatec.gisa.dtos.assistentesocial.response.AssistenteSocialResumoDTO;
import com.fatec.gisa.dtos.paciente.response.EscolaResponseDTO;
import com.fatec.gisa.enums.StatusPaciente;

public record PacienteDetalheDTO(
        PessoaResponseDTO pessoa,
        StatusPaciente statusPaciente,
        LocalDate dataCadastro,
        Boolean convenio,
        EscolaResponseDTO escola,
        List<CidResponseDTO> cids,
        ProntuarioResponseDTO prontuario,
        List<ResponsavelVinculoResponseDTO> responsaveis,
        // Vínculos ativos com assistentes sociais, ordenados pelo nome
        List<AssistenteSocialResumoDTO> assistentesSociais) {
}
