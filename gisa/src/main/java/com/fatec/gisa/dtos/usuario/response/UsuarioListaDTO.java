package com.fatec.gisa.dtos.usuario.response;

import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;

// Item da listagem de usuários de acesso (GET /api/usuarios)
public record UsuarioListaDTO(
        Long idCadastro,
        String nome,
        String cpf,
        TipoCadastro tipoCadastro,
        PerfilResponseDTO perfil,
        StatusCadastro statusCadastro) {
}
