package com.fatec.gisa.dtos.especialista.response;

import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;

// Especialista listado nos detalhes da especialidade; tipo é ESPECIALISTA ou ESPECIALISTA_PJ
public record EspecialistaVinculadoDTO(
        Long idCadastro,
        String nome,
        TipoCadastro tipo,
        StatusCadastro statusCadastro) {
}
