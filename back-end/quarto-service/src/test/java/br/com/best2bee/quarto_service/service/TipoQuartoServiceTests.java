package br.com.best2bee.quarto_service.service;

import java.util.Optional;

import br.com.best2bee.quarto_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.quarto_service.model.TipoQuarto;
import br.com.best2bee.quarto_service.repository.QuartoRepository;
import br.com.best2bee.quarto_service.repository.TipoQuartoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipoQuartoServiceTests {
    private final TipoQuartoRepository tipos = mock(TipoQuartoRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    private final QuartoRepository quartos = mock(QuartoRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    private final TipoQuartoService service = new TipoQuartoService(tipos, quartos);

    @Test
    void impedeExclusaoDeTipoEmUso() {
        when(tipos.findById(2)).thenReturn(Optional.of(new TipoQuarto()));
        when(quartos.existsByTipo_Id(2)).thenReturn(true);

        var erro = assertThrows(ResponseStatusException.class, () -> service.apagarTipoQuarto(2));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(tipos, never()).deleteById(anyInt());
        verify(tipos, never()).delete(any());
    }

    @Test
    void excluiTipoSemQuartosAssociados() {
        when(tipos.findById(2)).thenReturn(Optional.of(new TipoQuarto()));
        when(quartos.existsByTipo_Id(2)).thenReturn(false);

        service.apagarTipoQuarto(2);

        verify(tipos).deleteById(2);
    }

    @Test
    void rejeitaExclusaoDeTipoInexistente() {
        when(tipos.findById(2)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.apagarTipoQuarto(2));

        verify(tipos, never()).deleteById(anyInt());
        verifyNoInteractions(quartos);
    }
}
