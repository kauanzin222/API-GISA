package com.fatec.gisa.dtos.profissional.response;

import com.fatec.gisa.enums.TipoCadastro;

public record ProfissionalResumoDTO(
    Long idProfissional,
    String nome,
    String nomeCargo,
    String registroConselho,
    String statusCadastro,
    // PROFISSIONAL, ESPECIALISTA ou ESPECIALISTA_PJ: indica qual rota de detalhes usar
    TipoCadastro tipo
) {}
