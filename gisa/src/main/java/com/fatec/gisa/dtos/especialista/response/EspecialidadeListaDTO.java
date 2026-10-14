package com.fatec.gisa.dtos.especialista.response;

// Item de GET /api/especialidades. quantidadeEspecialistas conta especialistas e PJ, ativos e inativos
public record EspecialidadeListaDTO(
        Long idEspecialidade,
        String nome,
        String descricao,
        long quantidadeEspecialistas) {
}
