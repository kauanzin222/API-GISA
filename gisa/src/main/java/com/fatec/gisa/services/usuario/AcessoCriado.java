package com.fatec.gisa.services.usuario;

import com.fatec.gisa.entities.usuario.Usuario;

// Usuário criado ao conceder acesso e a senha temporária a enviar por e-mail
public record AcessoCriado(Usuario usuario, SenhaTemporariaGerada senhaTemporaria) {
}
