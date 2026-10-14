package com.fatec.gisa.dtos.especialista.response;

import java.util.List;

public record EspecialidadeDetalheDTO(
        Long idEspecialidade,
        String nome,
        String descricao,
        long quantidadeEspecialistas,
        List<EspecialistaVinculadoDTO> especialistas) {
}
