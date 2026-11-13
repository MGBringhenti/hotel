package br.com.best2bee.quarto_service.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration 
public class CorsConfig {
    
    @Bean 
    public CorsConfigurationSource corsConfigurationSource(@Value("${hotel.cors.origens}") String origens){
        List<String> origensPermitidas = Arrays.stream(origens.split(","))
        .map(String::strip)
        .filter(origem -> !origem.isEmpty())
        .toList();

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept"
        ));

        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
