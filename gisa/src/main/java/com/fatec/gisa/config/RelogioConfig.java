package com.fatec.gisa.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O servidor (Render) roda em UTC; "agora" e "hoje" para as terapias são os de Sorocaba
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
