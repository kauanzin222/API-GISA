package com.fatec.gisa.dtos.assistentesocial.response;

// Assistente social vinculado a um paciente (GET /api/pacientes/{id})
public record AssistenteSocialResumoDTO(
        Long idCadastro,
        String nome) {
}
