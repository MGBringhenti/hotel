package br.com.best2bee.cliente_service.service;

import br.com.best2bee.cliente_service.dto.RedefinirSenhaDTO;
import br.com.best2bee.cliente_service.dto.SolicitarRecuperacaoSenhaDTO;
import br.com.best2bee.cliente_service.model.RecuperacaoSenha;
import br.com.best2bee.cliente_service.repository.RecuperacaoSenhaRepository;
import br.com.best2bee.cliente_service.repository.UsuarioRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

@Service
public class RecuperacaoSenhaService {

    private static final Long VALIDADE_SEGUNDOS = 1800L;
    private static final long INTERVALO_SOLICITACOES_SEGUNDOS = 60L;

    private final SecureRandom secureRandom = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RecuperacaoSenhaRepository recuperacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
            RecuperacaoSenhaRepository recuperacaoRepository, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher publisher) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacaoRepository = recuperacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = publisher;
    }

    @Transactional
    public void solicitar(SolicitarRecuperacaoSenhaDTO dto) {
        String email = dto.emailRecuperacao()
                .strip()
                .toLowerCase(Locale.ROOT);

        var usuarioEncontrado = usuarioRepository.buscarParaRecuperacao(email);

        if (usuarioEncontrado.isEmpty()) {
            return;
        }

        var usuario = usuarioEncontrado.get();

        if (!usuario.isAtivo()) {
            return;
        }

        Instant agora = Instant.now();

        Instant limite = agora.minusSeconds(
                INTERVALO_SOLICITACOES_SEGUNDOS);

        boolean possuiPedidoRecente = recuperacaoRepository
                .existsByUsuario_IdAndCriadoEmAfter(
                        usuario.getId(),
                        limite);

        if (possuiPedidoRecente) {
            return;
        }

        String token = gerarToken();

        RecuperacaoSenha recuperacao = new RecuperacaoSenha();

        recuperacao.setUsuario(usuario);
        recuperacao.setUsuarioVersao(usuario.getVersao());
        recuperacao.setTokenHash(calcularHash(token));
        recuperacao.setCriadoEm(agora);
        recuperacao.setExpiraEm(
                agora.plusSeconds(VALIDADE_SEGUNDOS));

        recuperacaoRepository.saveAndFlush(recuperacao);

        eventPublisher.publishEvent(
                new RecuperacaoSenhaSolicitada(
                        usuario.getEmailRecuperacao(),
                        token));
    }

    @Transactional
    public void redefinir(RedefinirSenhaDTO dto) {
        String token = dto.token();

        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw tokenInvalido();
        }

        var recuperacao = recuperacaoRepository.buscarParaUtilizacao(calcularHash(token))
                .orElseThrow(this::tokenInvalido);

        Instant agora = Instant.now();

        if (recuperacao.getUtilizadoEm() != null || !agora.isBefore(recuperacao.getExpiraEm())) {
            throw tokenInvalido();
        }

        var usuario = recuperacao.getUsuario();

        if (!usuario.isAtivo() || !Objects.equals(usuario.getVersao(), recuperacao.getUsuarioVersao())) {
            throw tokenInvalido();
        }

        validarSenhaNova(dto.senhaNova());

        if (passwordEncoder.matches(
                dto.senhaNova(),
                usuario.getSenha())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A senha nova deve ser diferente da atual");
        }

        usuario.setSenha(
                passwordEncoder.encode(dto.senhaNova()));

        recuperacao.setUtilizadoEm(agora);

        usuarioRepository.saveAndFlush(usuario);
        recuperacaoRepository.saveAndFlush(recuperacao);
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String calcularHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 indisponível",
                    ex);
        }
    }

    private void validarSenhaNova(String senha) {
        if (senha == null
                || senha.isBlank()
                || senha.length() < 8
                || senha.length() > 50) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A senha deve ter entre 8 e 50 caracteres");
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A senha deve ocupar no máximo 72 bytes em UTF-8");
        }
    }

    private ResponseStatusException tokenInvalido() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Token de recuperação inválido ou expirado");
    }
}