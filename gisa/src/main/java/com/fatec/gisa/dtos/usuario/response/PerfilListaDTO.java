package com.fatec.gisa.dtos.usuario.response;

public record PerfilListaDTO(
        Long idPerfil,
        String nome,
        int quantidadePermissoes,
        long quantidadeUsuarios) {
}
