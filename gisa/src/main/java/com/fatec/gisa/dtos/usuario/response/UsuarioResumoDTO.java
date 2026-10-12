package com.fatec.gisa.dtos.usuario.response;

import java.util.List;

public record UsuarioResumoDTO(
        Long idCadastro,
        PerfilResponseDTO perfil,
        List<PermissaoResumoDTO> permissoes) {
}
