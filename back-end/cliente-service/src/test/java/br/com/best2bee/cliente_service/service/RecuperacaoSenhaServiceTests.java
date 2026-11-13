package br.com.best2bee.cliente_service.service;

import br.com.best2bee.cliente_service.dto.*;
import br.com.best2bee.cliente_service.model.*;
import br.com.best2bee.cliente_service.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RecuperacaoSenhaServiceTests {
    final UsuarioRepository usuarios = mock(UsuarioRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    final RecuperacaoSenhaRepository pedidos = mock(RecuperacaoSenhaRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class, withSettings().mockMaker("mock-maker-subclass"));
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final RecuperacaoSenhaService service = new RecuperacaoSenhaService(usuarios, pedidos, encoder, eventos);
    final String token = "a".repeat(43);

    Usuario usuario() {
        var u = new Usuario();
        u.setId(1); u.setVersao(2L); u.setAtivo(true);
        u.setEmailRecuperacao("teste@hotel.test");
        u.setSenha(encoder.encode("SenhaAnterior123"));
        return u;
    }
    RecuperacaoSenha pedido() {
        var p = new RecuperacaoSenha();
        p.setUsuario(usuario()); p.setUsuarioVersao(2L);
        p.setCriadoEm(Instant.now().minusSeconds(10));
        p.setExpiraEm(Instant.now().plusSeconds(300));
        when(pedidos.buscarParaUtilizacao(anyString())).thenReturn(Optional.of(p));
        return p;
    }
    @Test void geraTokenAleatorioMasPersisteSomenteHash() throws Exception {
        var u = usuario();
        when(usuarios.buscarParaRecuperacao("teste@hotel.test")).thenReturn(Optional.of(u));
        service.solicitar(new SolicitarRecuperacaoSenhaDTO(" TESTE@HOTEL.TEST "));
        var pc = ArgumentCaptor.forClass(RecuperacaoSenha.class);
        var ec = ArgumentCaptor.forClass(RecuperacaoSenhaSolicitada.class);
        verify(pedidos).saveAndFlush(pc.capture());
        verify(eventos).publishEvent(ec.capture());
        var evento = ec.getValue(); var salvo = pc.getValue();
        assertTrue(evento.token().matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(evento.token(), salvo.getTokenHash());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(evento.token().getBytes(StandardCharsets.UTF_8))), salvo.getTokenHash());
        assertEquals(Duration.ofMinutes(30), Duration.between(salvo.getCriadoEm(), salvo.getExpiraEm()));
        assertEquals(2L, salvo.getUsuarioVersao());
    }
    @Test void pedidoRecenteNaoGeraNovoEnvio() {
        when(usuarios.buscarParaRecuperacao(anyString())).thenReturn(Optional.of(usuario()));
        when(pedidos.existsByUsuario_IdAndCriadoEmAfter(eq(1), any())).thenReturn(true);
        service.solicitar(new SolicitarRecuperacaoSenhaDTO("teste@hotel.test"));
        verify(pedidos, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }
    @Test void contaInexistenteNaoGeraPedido() {
        when(usuarios.buscarParaRecuperacao(anyString())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.solicitar(new SolicitarRecuperacaoSenhaDTO("ausente@hotel.test")));
        verifyNoInteractions(pedidos, eventos);
    }
    @Test void contaInativaNaoGeraPedido() {
        var u = usuario(); u.setAtivo(false);
        when(usuarios.buscarParaRecuperacao(anyString())).thenReturn(Optional.of(u));
        service.solicitar(new SolicitarRecuperacaoSenhaDTO("teste@hotel.test"));
        verifyNoInteractions(pedidos, eventos);
    }
    @Test void redefineComBCryptEImpedeSegundaUtilizacao() {
        var p = pedido();
        service.redefinir(new RedefinirSenhaDTO(token, "SenhaNova123"));
        assertTrue(encoder.matches("SenhaNova123", p.getUsuario().getSenha()));
        assertNotNull(p.getUtilizadoEm());
        assertThrows(ResponseStatusException.class, () -> service.redefinir(new RedefinirSenhaDTO(token, "OutraSenha123")));
        verify(usuarios, times(1)).saveAndFlush(any());
    }
    @Test void rejeitaTokenExpiradoSemAlterarSenha() {
        var p = pedido(); p.setExpiraEm(Instant.now().minusSeconds(1));
        assertThrows(ResponseStatusException.class, () -> service.redefinir(new RedefinirSenhaDTO(token, "SenhaNova123")));
        verifyNoInteractions(usuarios);
    }
    @Test void rejeitaPedidoDeVersaoAnterior() {
        var p = pedido(); p.getUsuario().setVersao(3L);
        assertThrows(ResponseStatusException.class, () -> service.redefinir(new RedefinirSenhaDTO(token, "SenhaNova123")));
        verifyNoInteractions(usuarios);
    }
    @Test void rejeitaSenhaIgualEExcessoDeBytesSemConsumirToken() {
        var p = pedido();
        assertThrows(ResponseStatusException.class, () -> service.redefinir(new RedefinirSenhaDTO(token, "SenhaAnterior123")));
        assertThrows(ResponseStatusException.class, () -> service.redefinir(new RedefinirSenhaDTO(token, "é".repeat(40))));
        assertNull(p.getUtilizadoEm());
        verifyNoInteractions(usuarios);
    }
    @Test void rejeitaFormatoInvalidoSemConsultarBanco() {
        assertThrows(ResponseStatusException.class, () -> service.redefinir(new RedefinirSenhaDTO("invalido", "SenhaNova123")));
        verifyNoInteractions(pedidos, usuarios);
    }
}

