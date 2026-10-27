package com.fatec.gisa.services.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Conta tentativas por chave (ex.: "login:" + e-mail) numa janela fixa, para frear força bruta no
// login e o envio repetido de e-mails no "esqueci minha senha". Fica em memória: vale por instância
// da API e zera quando ela reinicia
@Component
public class LimiteTentativas {

    // Acima disso, cada registro também descarta as janelas vencidas (evita crescer sem limite)
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private record Janela(Instant inicio, int tentativas) {
    }

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock relogio;
    private final Duration duracao;

    public LimiteTentativas(Clock relogio,
            @Value("${gisa.auth.janela-tentativas-minutos:15}") long minutos) {
        this.relogio = relogio;
        this.duracao = Duration.ofMinutes(minutos);
    }

    public boolean excedeu(String chave, int maximo) {
        Janela janela = janelas.get(chave);
        return janela != null && !vencida(janela, relogio.instant()) && janela.tentativas() >= maximo;
    }

    public void registrar(String chave) {
        Instant agora = relogio.instant();
        janelas.compute(chave, (k, atual) -> atual == null || vencida(atual, agora)
                ? new Janela(agora, 1)
                : new Janela(atual.inicio(), atual.tentativas() + 1));
        if (janelas.size() > LIMPAR_ACIMA_DE) {
            janelas.values().removeIf(janela -> vencida(janela, agora));
        }
    }

    public void limpar(String chave) {
        janelas.remove(chave);
    }

    private boolean vencida(Janela janela, Instant agora) {
        return !janela.inicio().plus(duracao).isAfter(agora);
    }
}
