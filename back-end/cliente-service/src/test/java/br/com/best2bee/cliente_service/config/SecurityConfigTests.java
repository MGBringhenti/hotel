package br.com.best2bee.cliente_service.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTests {

    private static KeyPair chaves;
    private final SecurityConfig config = new SecurityConfig();

    @BeforeAll
    static void gerarChavesParaTestes() throws Exception {
        chaves = gerarChaves();
    }

    @Test
    void deveAceitarTokenValido() {
        String token = gerarToken(chaves, "hotel-auth", Instant.now().plusSeconds(300), "ADMIN");

        Jwt jwt = decoder().decode(token);

        assertEquals("hotel-auth", jwt.getClaimAsString("iss"));
        assertEquals("usuario-teste", jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("funcao"));
    }

    @Test
    void deveConverterFuncaoEmPermissao() {
        String token = gerarToken(chaves, "hotel-auth", Instant.now().plusSeconds(300), "RECEPCIONISTA");
        Jwt jwt = decoder().decode(token);

        var autenticacao = config.jwtAuthenticationConverter().convert(jwt);

        assertNotNull(autenticacao);
        assertEquals("usuario-teste", autenticacao.getName());
        var permissoes = autenticacao.getAuthorities().stream()
                .map(permissao -> permissao.getAuthority())
                .filter(permissao -> permissao.startsWith("ROLE_"))
                .toList();
        assertEquals(java.util.List.of("ROLE_RECEPCIONISTA"), permissoes);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        String token = gerarToken(chaves, "hotel-auth", Instant.now().minusSeconds(120), "ADMIN");
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void deveRejeitarEmissorIncorreto() {
        String token = gerarToken(chaves, "outro-emissor", Instant.now().plusSeconds(300), "ADMIN");
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void deveRejeitarAssinaturaDeOutraChave() throws Exception {
        String token = gerarToken(gerarChaves(), "hotel-auth", Instant.now().plusSeconds(300), "ADMIN");
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    private static KeyPair gerarChaves() throws Exception {
        var gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        return gerador.generateKeyPair();
    }

    private JwtDecoder decoder() {
        return config.jwtDecoder((RSAPublicKey) chaves.getPublic(), "hotel-auth");
    }

    private String gerarToken(KeyPair parDeChaves, String emissor, Instant expiracao, String funcao) {
        var encoder = config.jwtEncoder((RSAPublicKey) parDeChaves.getPublic(),
                (RSAPrivateKey) parDeChaves.getPrivate());
        var claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .subject("usuario-teste")
                .issuedAt(Instant.now().minusSeconds(300))
                .expiresAt(expiracao)
                .claim("funcao", funcao)
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
