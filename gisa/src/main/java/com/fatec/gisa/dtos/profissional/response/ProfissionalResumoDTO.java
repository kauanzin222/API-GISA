package com.fatec.gisa.dtos.profissional.response;

import com.fatec.gisa.enums.TipoCadastro;

public record ProfissionalResumoDTO(
    Long idProfissional,
    String nome,
    String nomeCargo,
    String registroConselho,
    String statusCadastro,
    // PROFISSIONAL, ESPECIALISTA, ESPECIALISTA_PJ ou ASSISTENTE_SOCIAL: indica qual rota de detalhes usar
    TipoCadastro tipo
) {}
