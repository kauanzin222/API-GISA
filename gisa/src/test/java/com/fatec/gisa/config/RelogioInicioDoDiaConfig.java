package com.fatec.gisa.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Relógio parado na meia-noite de hoje: terapias de hoje ainda podem ser agendadas (o horário
// é futuro) e já aceitam presença (é o dia da terapia), independente da hora em que o teste roda
@TestConfiguration
public class RelogioInicioDoDiaConfig {

    @Bean
    @Primary
    public Clock relogioInicioDoDia() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        return Clock.fixed(LocalDate.now(fuso).atStartOfDay(fuso).toInstant(), fuso);
    }
}
