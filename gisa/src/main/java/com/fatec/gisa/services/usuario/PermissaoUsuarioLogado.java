package com.fatec.gisa.services.usuario;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

// Verificação de permissão que depende do corpo ou dos dados (ex.: idPerfil no PATCH de
// profissional). As rotas em si são protegidas por @PreAuthorize nos controllers.
public final class PermissaoUsuarioLogado {

    public static final String ACESSO_TOTAL = "ACESSO_TOTAL";

    private PermissaoUsuarioLogado() {
    }

    // ACESSO_TOTAL vale por todas as permissões
    public static boolean possui(String permissao) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(nome -> nome.equals(ACESSO_TOTAL) || nome.equals(permissao));
    }

    // Sem a permissão: 403 com a mensagem padrão (ver RestExceptionHandler)
    public static void exigir(String permissao) {
        if (!possui(permissao)) {
            throw new AccessDeniedException("Permissão exigida: " + permissao);
        }
    }
}
