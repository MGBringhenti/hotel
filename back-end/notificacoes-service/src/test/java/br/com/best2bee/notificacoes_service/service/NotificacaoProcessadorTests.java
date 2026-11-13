package br.com.best2bee.notificacoes_service.service;

import br.com.best2bee.notificacoes_service.client.ClienteInternoClient;
import br.com.best2bee.notificacoes_service.dto.*;
import br.com.best2bee.notificacoes_service.model.*;
import br.com.best2bee.notificacoes_service.repository.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NotificacaoProcessadorTests {
    final NotificacaoRepository repo = mock(NotificacaoRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    final EventoRecebidoRepository eventos = mock(EventoRecebidoRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    final ClienteInternoClient cliente = mock(ClienteInternoClient.class, withSettings().mockMaker("mock-maker-subclass"));
    final EmailService email = mock(EmailService.class, withSettings().mockMaker("mock-maker-subclass"));
    final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    final Clock clock = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    final NotificacaoProcessador service = new NotificacaoProcessador(repo,eventos,cliente,email,json,clock);
    Notificacao preparar(int tentativas) {
        var n = new Notificacao(); n.setEventoId(UUID.randomUUID()); n.setClienteId(1); n.setTentativas(tentativas);
        var e = mock(EventoRecebido.class, withSettings().mockMaker("mock-maker-subclass"));
        when(e.getReservaId()).thenReturn(10); when(e.getReservaVersao()).thenReturn(1L);
        var dados = new ReservaEventoDTO(1,10,1L,1,2,LocalDate.of(2026,12,10),LocalDate.of(2026,12,12),
            new BigDecimal("150.00"),new BigDecimal("300.00"),"PENDENTE");
        when(e.getPayload()).thenReturn(json.writeValueAsString(dados));
        when(repo.buscarProximaParaEnviar(any())).thenReturn(Optional.of(n));
        when(eventos.findById(n.getEventoId())).thenReturn(Optional.of(e));
        when(cliente.buscarDestinatario(1)).thenReturn(new DestinatarioDTO(1,"Teste","teste@hotel.test",true));
        return n;
    }
    @Test void filaVaziaNaoEnvia() {
        when(repo.buscarProximaParaEnviar(any())).thenReturn(Optional.empty());
        assertFalse(service.processarProximo()); verifyNoInteractions(email,cliente);
    }
    @Test void sucessoMarcaEnvioEIncrementaTentativa() {
        var n=preparar(0);
        assertTrue(service.processarProximo());
        assertEquals(clock.instant(),n.getEnviadoEm()); assertEquals(1,n.getTentativas());
        assertNull(n.getDescartadoEm());
        verify(email).enviar(eq("teste@hotel.test"),contains("#10"),contains("300,00"));
    }
    @Test void falhaTemporariaReagendaSemMarcarEnvio() {
        var n=preparar(0);
        doThrow(new IllegalStateException("SMTP de teste indisponivel")).when(email).enviar(anyString(),anyString(),anyString());
        assertTrue(service.processarProximo());
        assertEquals(clock.instant().plusSeconds(5),n.getProximaTentativaEm());
        assertNull(n.getEnviadoEm()); assertNull(n.getDescartadoEm());
    }
    @Test void decimaFalhaInterrompeTentativas() {
        var n=preparar(9);
        doThrow(new IllegalStateException("SMTP de teste indisponivel")).when(email).enviar(anyString(),anyString(),anyString());
        service.processarProximo();
        assertEquals(10,n.getTentativas());
        assertEquals("LIMITE_TENTATIVAS",n.getMotivoDescarte());
        assertEquals(clock.instant(),n.getDescartadoEm()); assertNull(n.getEnviadoEm());
    }
    @Test void sucessoNaDecimaTentativaNaoDescarta() {
        var n=preparar(9); service.processarProximo();
        assertNotNull(n.getEnviadoEm()); assertNull(n.getDescartadoEm());
    }
    @Test void eventoSuperadoNaoConsultaClienteNemEnvia() {
        var n=preparar(0);
        when(eventos.existsByReservaIdAndReservaVersaoGreaterThan(10,1L)).thenReturn(true);
        service.processarProximo();
        assertEquals("EVENTO_SUPERADO",n.getMotivoDescarte());
        assertEquals(0,n.getTentativas());
        verifyNoInteractions(email); verify(cliente,never()).buscarDestinatario(anyInt());
    }
}

