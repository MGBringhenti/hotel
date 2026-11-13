package br.com.best2bee.cliente_service.service;

import java.time.Instant;

import br.com.best2bee.cliente_service.dto.LoginDTO;
import br.com.best2bee.cliente_service.dto.TokenDTO;
import br.com.best2bee.cliente_service.enums.Funcao;
import br.com.best2bee.cliente_service.repository.ClienteRepository;
import br.com.best2bee.cliente_service.repository.UsuarioRepository;
import br.com.best2bee.cliente_service.dto.AlterarSenhaDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public AuthService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${jwt.issuer}") String issuer) {

        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    public TokenDTO login(LoginDTO dto) {
        
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw credenciaisInvalidas();
        }
        var usuario = usuarioRepository.findByLogin(dto.login())
                .orElseThrow(this::credenciaisInvalidas);

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw credenciaisInvalidas();
        }

        if (!usuario.isAtivo()) {
            throw credenciaisInvalidas();
        }

        Instant agora = Instant.now();
        long validade = 7200L;

        var builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(validade))
                .claim("funcao", usuario.getFuncao().name());

        if (usuario.getFuncao() == Funcao.CLIENTE) {
            var cliente = clienteRepository
                    .findByUsuario_Id(usuario.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Conta sem cliente associado"));

            if (!cliente.isAtivo()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cliente inativo");
            }

            builder.claim("clienteId", cliente.getId());
        }

        var claims = builder.build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new TokenDTO(token, "Bearer", validade);
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciais inválidas");
    }

    @Transactional
    public void alterarSenha(String usuarioId, AlterarSenhaDTO dto) {

        Integer id;

        try {
            id = Integer.valueOf(usuarioId);
        } catch (NumberFormatException ex) {
            throw credenciaisInvalidas();
        }
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(this::credenciaisInvalidas);

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta inativa");
        }

        if (usuario.getFuncao() == Funcao.CLIENTE) {
            var cliente = clienteRepository.findByUsuario_Id(id)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta sem cliente associado"));

            if (!cliente.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente inativo");
            }
        }

        if (dto.senhaAtual().getBytes(StandardCharsets.UTF_8).length > 72
                || dto.senhaNova().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ocupar no máximo 72 bytes em UTF-8");
        }

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A sua senha está incorreta");
        }

        if (passwordEncoder.matches(dto.senhaNova(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha nova deve ser diferente da atual");
        }

        usuario.setSenha(passwordEncoder.encode(dto.senhaNova()));
        usuarioRepository.saveAndFlush(usuario);
    }
}