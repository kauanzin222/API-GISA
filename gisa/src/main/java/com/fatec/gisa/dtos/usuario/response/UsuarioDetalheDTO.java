package com.fatec.gisa.dtos.usuario.response;

import java.util.List;

import com.fatec.gisa.dtos.profissional.response.CargoResumoDTO;
import com.fatec.gisa.enums.StatusCadastro;
import com.fatec.gisa.enums.TipoCadastro;

// Detalhes do acesso. email e cargo vêm null para o tipo PESSOA (administrador inicial)
public record UsuarioDetalheDTO(
        Long idCadastro,
        String nome,
        String cpf,
        String email,
        TipoCadastro tipoCadastro,
        StatusCadastro statusCadastro,
        CargoResumoDTO cargo,
        PerfilResponseDTO perfil,
        List<PermissaoResumoDTO> permissoes) {
}
