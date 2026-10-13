package com.fatec.gisa.dtos.usuario.response;

import java.util.List;

import com.fatec.gisa.enums.TipoCadastro;

// Quem está logado: nome para o cabeçalho e tipo/permissões para escolher o painel
public record UsuarioLogadoDTO(
        Long idCadastro,
        String nome,
        String cpf,
        TipoCadastro tipoCadastro,
        PerfilResponseDTO perfil,
        List<PermissaoResumoDTO> permissoes) {
}
