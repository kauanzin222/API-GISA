package com.fatec.gisa.dtos.usuario.response;

import java.util.List;

// Resposta de POST .../{idCadastro}/usuario. emailEnviado = false quando o e-mail com a senha
// temporária não pôde ser enviado: o acesso foi criado e o front oferece o reenvio
// (POST /api/usuarios/{idCadastro}/senha-temporaria)
public record AcessoConcedidoDTO(
        Long idCadastro,
        PerfilResponseDTO perfil,
        List<PermissaoResumoDTO> permissoes,
        boolean emailEnviado) {

    public static AcessoConcedidoDTO de(UsuarioResumoDTO usuario, boolean emailEnviado) {
        return new AcessoConcedidoDTO(usuario.idCadastro(), usuario.perfil(), usuario.permissoes(), emailEnviado);
    }
}
