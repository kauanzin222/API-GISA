package com.fatec.gisa.dtos.profissional.response;

public record ProfissionalResumoDTO(
    Long idProfissional,
    String nome,
    String nomeCargo,
    String registroConselho,
    String statusCadastro
) {}
