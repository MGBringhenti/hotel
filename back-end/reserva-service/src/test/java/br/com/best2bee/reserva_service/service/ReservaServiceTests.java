package br.com.best2bee.reserva_service.service;

import br.com.best2bee.reserva_service.client.*;
import br.com.best2bee.reserva_service.dto.*;
import br.com.best2bee.reserva_service.model.Reserva;
import br.com.best2bee.reserva_service.repository.ReservaRepository;
import br.com.best2bee.reserva_service.enums.*;
import br.com.best2bee.reserva_service.security.Ator;
import br.com.best2bee.reserva_service.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReservaServiceTests {
    final ReservaRepository repo = mock(ReservaRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    final QuartoClient quartos = mock(QuartoClient.class, withSettings().mockMaker("mock-maker-subclass"));
    final ClienteClient clientes = mock(ClienteClient.class, withSettings().mockMaker("mock-maker-subclass"));
    final OutboxService outbox = mock(OutboxService.class, withSettings().mockMaker("mock-maker-subclass"));
    final Clock clock = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    final ReservaService service = new ReservaService(repo, quartos, clientes, clock, outbox);
    final LocalDate entrada = LocalDate.of(2026,12,10);
    final LocalDate saida = entrada.plusDays(2);
    void dependencias() {
        when(clientes.buscarPorId(1, "token")).thenReturn(new ClienteConsultaDTO(1, true));
        when(quartos.buscarPorId(2, "token")).thenReturn(new QuartoConsultaDTO(2, "DISPONIVEL", new TipoQuartoConsultaDTO(new BigDecimal("150.00"))));
        when(repo.saveAndFlush(any())).thenAnswer(i -> {
            Reserva r = i.getArgument(0); r.setIdReserva(10); r.setVersao(r.getVersao() == null ? 0L : r.getVersao()+1); return r;
        });
    }
    Reserva existente() {
        var r = new Reserva(); r.setIdReserva(10); r.setIdCliente(1); r.setIdQuarto(2);
        r.setDataInicio(entrada); r.setDataFinal(saida); r.setVersao(0L);
        r.setStatus(Status.PENDENTE); r.setPrecoDiarioAplicado(new BigDecimal("100.00"));
        when(repo.findById(10)).thenReturn(Optional.of(r)); return r;
    }
    @Test void calculaDiariasERegistraEvento() {
        dependencias();
        var r = service.postReserva(new CriarReservaDTO(1,2,entrada,saida), "token");
        assertEquals(new BigDecimal("300.00"), r.getValorTotal());
        assertEquals(Status.PENDENTE, r.getStatus());
        verify(outbox).registrar(r, TipoEventoReserva.RESERVA_CRIADA);
    }
    @Test void rejeitaPeriodoInvalidoAntesDeConsultarDependencias() {
        assertThrows(ResponseStatusException.class, () -> service.postReserva(new CriarReservaDTO(1,2,entrada,entrada), "token"));
        assertThrows(ResponseStatusException.class, () -> service.postReserva(new CriarReservaDTO(1,2,LocalDate.of(2026,11,12),saida), "token"));
        verifyNoInteractions(clientes, quartos, outbox);
    }
    @Test void rejeitaSobreposicaoSemSalvar() {
        dependencias();
        when(repo.contarConflitos(eq(2), eq(entrada), eq(saida), anyList(), isNull())).thenReturn(1L);
        assertThrows(ResponseStatusException.class, () -> service.postReserva(new CriarReservaDTO(1,2,entrada,saida), "token"));
        verify(repo, never()).saveAndFlush(any()); verifyNoInteractions(outbox);
    }
    @Test void rejeitaQuartoInativo() {
        dependencias();
        when(quartos.buscarPorId(2,"token")).thenReturn(new QuartoConsultaDTO(2,"INATIVO",null));
        assertThrows(ResponseStatusException.class, () -> service.postReserva(new CriarReservaDTO(1,2,entrada,saida), "token"));
        verify(repo, never()).saveAndFlush(any());
    }
    @Test void alterarDatasPreservaDiariaContratada() {
        dependencias(); existente();
        var r = service.putReserva(10, new AlterarReservaDTO(1,2,entrada,saida.plusDays(1),0L), "token");
        assertEquals(new BigDecimal("100.00"), r.getPrecoDiarioAplicado());
        assertEquals(new BigDecimal("300.00"), r.getValorTotal());
        verify(outbox).registrar(r, TipoEventoReserva.RESERVA_ALTERADA);
    }
    @Test void versaoDesatualizadaNaoAlteraReserva() {
        existente();
        assertThrows(ResponseStatusException.class, () -> service.putReserva(10, new AlterarReservaDTO(1,2,entrada,saida,5L), "token"));
        verifyNoInteractions(quartos, clientes, outbox);
    }
    @Test void clienteNaoConsultaReservaAlheia() {
        existente();
        assertThrows(RecursoNaoEncontradoException.class, () -> service.getReservaById(10,new Ator("9","CLIENTE",9)));
    }
    @Test void clienteNaoConfirmaMesmoSendoProprietario() {
        existente();
        assertThrows(ResponseStatusException.class, () -> service.alterarStatus(10,new AlterarStatusDTO(Status.CONFIRMADA,0L),new Ator("1","CLIENTE",1),"token"));
        verify(repo, never()).saveAndFlush(any());
    }
    @Test void cancelamentoDoProprietarioPreservaPreco() {
        dependencias(); var r=existente();
        service.alterarStatus(10,new AlterarStatusDTO(Status.CANCELADA,0L),new Ator("1","CLIENTE",1),"token");
        assertEquals(Status.CANCELADA,r.getStatus());
        assertEquals(new BigDecimal("100.00"),r.getPrecoDiarioAplicado());
        verify(outbox).registrar(r,TipoEventoReserva.RESERVA_STATUS_ALTERADO);
    }
}

