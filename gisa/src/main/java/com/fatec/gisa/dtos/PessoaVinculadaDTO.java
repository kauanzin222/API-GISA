package com.fatec.gisa.dtos;

import com.fatec.gisa.enums.StatusCadastro;

// Pessoa listada nos detalhes de perfil (usuários) e de cargo (profissionais)
public record PessoaVinculadaDTO(
        Long idCadastro,
        String nome,
        String cpf,
        StatusCadastro statusCadastro) {
}
