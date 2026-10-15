package com.fatec.gisa.dtos.usuario.response;

public record PermissaoResumoDTO(
        Long idPermissao,
        String nome,
        String descricao,
        // Agrupamento na tela de perfis (ex.: "Pacientes", "Segurança")
        String categoria) {
}
