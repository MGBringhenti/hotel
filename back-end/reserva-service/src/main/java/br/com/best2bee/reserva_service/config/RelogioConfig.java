package br.com.best2bee.reserva_service.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
public class RelogioConfig {

    @Bean 
    public Clock clock(){
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
