package br.com.best2bee.notificacoes_service.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AgendamentoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}