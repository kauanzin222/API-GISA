package com.fatec.gisa.services.email;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import lombok.extern.slf4j.Slf4j;

// Envio dos e-mails de acesso, nesta ordem de preferência:
// 1. API HTTP do Brevo (BREVO_API_KEY): usa HTTPS, então funciona em hospedagens que bloqueiam as
//    portas de SMTP (como o plano gratuito do Render);
// 2. SMTP (SMTP_HOST);
// 3. sem nenhum dos dois (desenvolvimento local), o conteúdo vai para o log em vez de ser enviado.
// Nunca lança exceção: devolve false se o envio falhar, e quem chamou decide o que fazer
// (ex.: responder emailEnviado: false).
@Slf4j
@Service
public class EmailService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";
    private static final Duration TEMPO_LIMITE_BREVO = Duration.ofSeconds(10);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String brevoApiKey;
    private final String remetente;
    private final String nomeRemetente;
    private final String frontUrl;
    private final RestClient brevo;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${gisa.email.brevo-api-key:}") String brevoApiKey,
            @Value("${gisa.email.remetente}") String remetente,
            @Value("${gisa.email.remetente-nome}") String nomeRemetente,
            @Value("${gisa.front-url}") String frontUrl) {
        this.mailSender = mailSender;
        this.host = host;
        this.brevoApiKey = brevoApiKey;
        this.remetente = remetente;
        this.nomeRemetente = nomeRemetente;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;

        SimpleClientHttpRequestFactory requisicoes = new SimpleClientHttpRequestFactory();
        requisicoes.setConnectTimeout(TEMPO_LIMITE_BREVO);
        requisicoes.setReadTimeout(TEMPO_LIMITE_BREVO);
        this.brevo = RestClient.builder().requestFactory(requisicoes).build();
    }

    // Boas-vindas com a senha temporária (conceder acesso ou nova senha temporária)
    public boolean enviarSenhaTemporaria(String nome, String email, String senha, Instant expiraEm) {
        String texto = """
                Olá, %s.

                Seu acesso ao GISA está pronto.

                E-mail de login: %s
                Senha temporária: %s

                A senha temporária vale até %s (7 dias). No primeiro acesso, o sistema vai pedir que você cadastre uma senha nova.

                Acesse: %s

                Se a senha expirar, use "Esqueci minha senha" na tela de login ou peça um novo acesso ao administrador.
                Se você não esperava este e-mail, ignore-o.
                """.formatted(nome, email, senha, DATA_HORA.format(expiraEm), frontUrl);
        return enviar(email, "GISA - Seu acesso ao sistema", texto);
    }

    public boolean enviarLinkRedefinicao(String nome, String email, String token, Duration validade) {
        String texto = """
                Olá, %s.

                Recebemos um pedido para redefinir a sua senha do GISA. Para cadastrar uma senha nova, acesse o link abaixo:

                %s/redefinir-senha?token=%s

                O link vale por %d minutos e só pode ser usado uma vez.

                Se você não pediu a redefinição, ignore este e-mail: a sua senha atual continua valendo.
                """.formatted(nome, frontUrl, token, validade.toMinutes());
        return enviar(email, "GISA - Redefinição de senha", texto);
    }

    private boolean enviar(String destinatario, String assunto, String texto) {
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            return enviarPeloBrevo(destinatario, assunto, texto);
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (host == null || host.isBlank() || sender == null) {
            log.info("E-mail não configurado (BREVO_API_KEY e SMTP_HOST vazios); e-mail não enviado.\n"
                    + "Para: {}\nAssunto: {}\n\n{}", destinatario, assunto, texto);
            return true;
        }
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(texto);
            sender.send(mensagem);
            return true;
        } catch (RuntimeException erro) {
            log.error("Falha ao enviar o e-mail \"{}\" para {}", assunto, destinatario, erro);
            return false;
        }
    }

    // POST https://api.brevo.com/v3/smtp/email (e-mail transacional). O remetente precisa estar
    // cadastrado e verificado no Brevo
    private boolean enviarPeloBrevo(String destinatario, String assunto, String texto) {
        Map<String, Object> corpo = Map.of(
                "sender", Map.of("email", remetente, "name", nomeRemetente),
                "to", List.of(Map.of("email", destinatario)),
                "subject", assunto,
                "textContent", texto);
        try {
            brevo.post()
                    .uri(URL_BREVO)
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException erro) {
            // A resposta do Brevo diz o motivo (ex.: chave inválida, remetente não verificado)
            log.error("Brevo recusou o e-mail \"{}\" para {}: {} {}", assunto, destinatario,
                    erro.getStatusCode(), erro.getResponseBodyAsString());
            return false;
        } catch (RuntimeException erro) {
            log.error("Falha ao enviar pelo Brevo o e-mail \"{}\" para {}", assunto, destinatario, erro);
            return false;
        }
    }
}
