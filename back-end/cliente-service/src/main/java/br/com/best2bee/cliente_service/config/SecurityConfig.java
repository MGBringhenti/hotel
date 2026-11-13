package br.com.best2bee.cliente_service.config;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
public class SecurityConfig {

    @Bean
    RSAPublicKey publicKey(
            @Value("${jwt.public-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(input);
        }
    }

    @Bean
    RSAPrivateKey privateKey(
            @Value("${jwt.private-key}") Resource resource) throws IOException {
        try (var input = resource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(input);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        var rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        return new NimbusJwtEncoder(
                new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            @Value("${jwt.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer));

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
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter) throws Exception {

        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/auth/login")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST,"/auth/recuperacao-senha", "/auth/recuperacao-senha/redefinir")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/auth/servicos/token")
                        .permitAll()

                        .requestMatchers(HttpMethod.PATCH, "/auth/senha")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")

                        .requestMatchers(HttpMethod.GET, "/usuarios", "/usuarios/*")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/usuarios")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/usuarios/*")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/usuarios/*/situacao")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/clientes")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")

                        .requestMatchers(HttpMethod.GET, "/clientes/*")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")

                        .requestMatchers(HttpMethod.POST, "/clientes")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")

                        .requestMatchers(HttpMethod.PUT, "/clientes/*")
                        .hasAnyRole("ADMIN", "RECEPCIONISTA")

                        .requestMatchers(HttpMethod.PUT, "/clientes/*/usuario")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/clientes/*/situacao")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/interno/clientes/*/destinatario")
                        .hasRole("SERVICO_NOTIFICACOES")

                        .anyRequest().denyAll())

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }
}