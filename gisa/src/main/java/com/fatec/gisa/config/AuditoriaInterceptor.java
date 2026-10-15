package com.fatec.gisa.config;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.fatec.gisa.enums.AcaoAuditoria;
import com.fatec.gisa.services.auditoria.AuditoriaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Registra na auditoria as alterações (POST, PATCH, PUT, DELETE), os logins que falharam e a
// consulta aos detalhes de um paciente (dados de saúde). Nenhum outro GET é registrado.
// Roda depois da resposta pronta, então o status gravado já é o final (inclusive de erros).
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {

    // O login ainda não tem usuário autenticado; o AuthController informa aqui o e-mail digitado
    // e, se ele pertencer a alguém, o CPF dessa pessoa
    public static final String ATRIBUTO_CPF_LOGIN = "auditoria.cpfLogin";
    public static final String ATRIBUTO_EMAIL_LOGIN = "auditoria.emailLogin";

    private static final String ROTA_LOGIN = "/api/auth/login";

    // Único GET auditado: acesso aos dados de saúde de um paciente (LGPD)
    private static final String CONSULTA_SENSIVEL = "/api/pacientes/{idCadastro}";

    private final AuditoriaService auditoriaService;

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler, Exception exception) {
        AcaoAuditoria acao = acao(request, response);
        if (acao == null) {
            return;
        }

        // Falha ao auditar nunca derruba a requisição
        try {
            auditoriaService.registrar(cpf(request), acao == AcaoAuditoria.LOGIN ? emailLogin(request) : null,
                    acao, request.getMethod(), request.getRequestURI(), response.getStatus(), ip(request));
        } catch (RuntimeException erro) {
            log.warn("Não foi possível registrar a auditoria de {} {}", request.getMethod(), request.getRequestURI(), erro);
        }
    }

    private AcaoAuditoria acao(HttpServletRequest request, HttpServletResponse response) {
        String metodo = request.getMethod();
        if ("POST".equals(metodo) && ROTA_LOGIN.equals(request.getRequestURI())) {
            // Logins bem-sucedidos não são registrados; só as tentativas que falharam
            return response.getStatus() >= 400 ? AcaoAuditoria.LOGIN : null;
        }
        return switch (metodo) {
            case "POST" -> AcaoAuditoria.CRIACAO;
            case "PUT", "PATCH" -> AcaoAuditoria.ALTERACAO;
            case "DELETE" -> AcaoAuditoria.EXCLUSAO;
            case "GET" -> CONSULTA_SENSIVEL.equals(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                    ? AcaoAuditoria.CONSULTA
                    : null;
            default -> null;
        };
    }

    private String cpf(HttpServletRequest request) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.isAuthenticated()
                && !(autenticacao instanceof AnonymousAuthenticationToken)) {
            return autenticacao.getName();
        }
        Object cpfLogin = request.getAttribute(ATRIBUTO_CPF_LOGIN);
        return cpfLogin == null ? null : cpfLogin.toString();
    }

    private String emailLogin(HttpServletRequest request) {
        Object email = request.getAttribute(ATRIBUTO_EMAIL_LOGIN);
        return email == null ? null : email.toString();
    }

    // Atrás do proxy do Render, o IP do cliente vem no primeiro valor de X-Forwarded-For
    private String ip(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
