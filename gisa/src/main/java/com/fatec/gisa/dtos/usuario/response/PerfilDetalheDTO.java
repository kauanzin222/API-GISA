package com.fatec.gisa.dtos.usuario.response;

import java.util.List;

import com.fatec.gisa.dtos.PessoaVinculadaDTO;

public record PerfilDetalheDTO(
        Long idPerfil,
        String nome,
        List<PermissaoResumoDTO> permissoes,
        List<PessoaVinculadaDTO> usuarios) {
}
