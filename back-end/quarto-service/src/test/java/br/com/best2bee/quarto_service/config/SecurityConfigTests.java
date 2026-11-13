package br.com.best2bee.quarto_service.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTests {

    private static KeyPair keyPair;
    private final SecurityConfig config = new SecurityConfig();

    @BeforeAll
    static void generateKeys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void acceptsSignedTokenAndConvertsRole() {
        var jwt = decoder().decode(token(keyPair, "hotel-auth", Instant.now().plusSeconds(300)));
        var authentication = config.jwtAuthenticationConverter().convert(jwt);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RECEPCIONISTA")));
        assertEquals("1", authentication.getName());
    }

    @Test
    void rejectsWrongIssuer() {
        assertThrows(JwtException.class, () -> decoder()
                .decode(token(keyPair, "outro-emissor", Instant.now().plusSeconds(300))));
    }

    @Test
    void rejectsExpiredToken() {
        assertThrows(JwtException.class, () -> decoder()
                .decode(token(keyPair, "hotel-auth", Instant.now().minusSeconds(120))));
    }

    @Test
    void rejectsSignatureFromAnotherKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var anotherKey = generator.generateKeyPair();
        assertThrows(JwtException.class, () -> decoder()
                .decode(token(anotherKey, "hotel-auth", Instant.now().plusSeconds(300))));
    }

    private JwtDecoder decoder() {
        return config.jwtDecoder((RSAPublicKey) keyPair.getPublic(), "hotel-auth");
    }

    private String token(KeyPair keys, String issuer, Instant expiration) {
        var rsa = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
                .privateKey((RSAPrivateKey) keys.getPrivate()).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
        var claims = JwtClaimsSet.builder().issuer(issuer).subject("1")
                .issuedAt(Instant.now().minusSeconds(300)).expiresAt(expiration)
                .claim("funcao", "RECEPCIONISTA").build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
