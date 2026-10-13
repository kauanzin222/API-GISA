package com.fatec.gisa.dtos.paciente.response;

import java.time.Instant;

import com.fatec.gisa.enums.TipoEventoPaciente;

public record EventoPacienteDTO(
        Long idEvento,
        // UTC
        Instant dataHora,
        TipoEventoPaciente tipo,
        String descricao,
        // Só nos eventos de terapia
        Long idTerapia,
        // Quem fez a ação; null se não houver usuário identificado
        UsuarioEventoDTO usuario) {

    public record UsuarioEventoDTO(Long idCadastro, String nome) {
    }
}
