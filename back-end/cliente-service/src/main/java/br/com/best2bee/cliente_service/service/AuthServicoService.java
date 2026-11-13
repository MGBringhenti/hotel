package br.com.best2bee.cliente_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.com.best2bee.cliente_service.dto.LoginServicoDTO;
import br.com.best2bee.cliente_service.dto.TokenDTO;

@Service
public class AuthServicoService {

    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String clientId;
    private final String segredoHash;

    public AuthServicoService(
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${jwt.issuer}") String issuer,
            @Value("${hotel.servicos.notificacoes.client-id}")
            String clientId,
            @Value("${hotel.servicos.notificacoes.client-secret}")
            String clientSecret
    ) {
        int tamanho = clientSecret.getBytes(StandardCharsets.UTF_8).length;

        if (clientSecret.isBlank() || tamanho < 32 || tamanho > 72) {
            throw new IllegalStateException(
                    "O segredo do notificacoes-service deve ter entre 32 e 72 bytes"
            );
        }

        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.clientId = clientId;
        this.segredoHash = passwordEncoder.encode(clientSecret);
    }

    public TokenDTO autenticar(LoginServicoDTO dto) {
        if (dto == null
                || dto.clientId() == null
                || dto.clientSecret() == null) {
            throw credenciaisInvalidas();
        }

        int tamanho = dto.clientSecret()
                .getBytes(StandardCharsets.UTF_8).length;

        if (tamanho < 32 || tamanho > 72) {
            throw credenciaisInvalidas();
        }

        boolean segredoValido = passwordEncoder.matches(
                dto.clientSecret(),
                segredoHash
        );

        if (!clientId.equals(dto.clientId()) || !segredoValido) {
            throw credenciaisInvalidas();
        }

        Instant agora = Instant.now();
        long validade = 300L;

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(clientId)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(validade))
                .claim("funcao", "SERVICO_NOTIFICACOES")
                .build();

        var header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new TokenDTO(token, "Bearer", validade);
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciais de serviço inválidas"
        );
    }
}