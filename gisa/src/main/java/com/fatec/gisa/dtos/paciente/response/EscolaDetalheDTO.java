package com.fatec.gisa.dtos.paciente.response;

import com.fatec.gisa.enums.TipoEscola;

// Item da listagem e detalhe da escola. quantidadePacientes conta ativos e inativos
public record EscolaDetalheDTO(
        Long idEscola,
        String nome,
        TipoEscola tipoEscola,
        String telefone,
        long quantidadePacientes) {
}
