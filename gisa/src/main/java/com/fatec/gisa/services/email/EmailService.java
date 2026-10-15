package com.fatec.gisa.services.email;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

// Envio dos e-mails de acesso. Sem SMTP configurado (SMTP_HOST vazio, como no desenvolvimento
// local), o conteúdo vai para o log em vez de ser enviado. Nunca lança exceção: devolve false se
// o envio falhar, e quem chamou decide o que fazer (ex.: responder emailEnviado: false).
@Slf4j
@Service
public class EmailService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String remetente;
    private final String frontUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${gisa.email.remetente}") String remetente,
            @Value("${gisa.front-url}") String frontUrl) {
        this.mailSender = mailSender;
        this.host = host;
        this.remetente = remetente;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
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
        JavaMailSender sender = mailSender.getIfAvailable();
        if (host == null || host.isBlank() || sender == null) {
            log.info("SMTP não configurado; e-mail não enviado.\nPara: {}\nAssunto: {}\n\n{}", destinatario, assunto, texto);
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
}
