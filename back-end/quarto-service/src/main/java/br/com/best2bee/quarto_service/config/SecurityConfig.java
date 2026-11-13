package br.com.best2bee.quarto_service.config;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
public class SecurityConfig {

    @Bean
    RSAPublicKey publicKey(@Value("${jwt.public-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(input);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey, @Value("${jwt.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("funcao");
        authorities.setAuthorityPrefix("ROLE_");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter converter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/quarto", "/quarto/*")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")

                        .requestMatchers(HttpMethod.POST, "/quarto")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")

                        .requestMatchers(HttpMethod.PUT, "/quarto/*")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")

                        .requestMatchers(HttpMethod.PATCH, "/quarto/*/status")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/tipoQuarto", "/tipoQuarto/*")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")

                        .requestMatchers(HttpMethod.POST, "/tipoQuarto")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/tipoQuarto/*")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/tipoQuarto/*")
                        .hasRole("ADMIN")

                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }
}
