package br.com.best2bee.quarto_service.service;

import java.util.Optional;

import br.com.best2bee.quarto_service.dto.QuartoDTO;
import br.com.best2bee.quarto_service.enums.QuartoStatus;
import br.com.best2bee.quarto_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.quarto_service.model.Quarto;
import br.com.best2bee.quarto_service.model.TipoQuarto;
import br.com.best2bee.quarto_service.repository.QuartoRepository;
import br.com.best2bee.quarto_service.repository.TipoQuartoRepository;
import br.com.best2bee.quarto_service.dto.AtualizarQuartoDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuartoServiceTests {
    private final QuartoRepository quartos = mock(QuartoRepository.class,
            withSettings().mockMaker("mock-maker-subclass"));
    private final TipoQuartoRepository tipos = mock(TipoQuartoRepository.class,
            withSettings().mockMaker("mock-maker-subclass"));
    private final QuartoService service = new QuartoService(quartos, tipos);

    @Test
    void cadastraQuartoComTipoExistente() {
        var tipo = new TipoQuarto();
        tipo.setId(2);
        when(tipos.findById(2)).thenReturn(Optional.of(tipo));
        when(quartos.save(any(Quarto.class))).thenAnswer(invocation -> {
            Quarto salvo = invocation.getArgument(0);
            salvo.setId(10);
            return salvo;
        });

        var resultado = service.criarQuarto(dto());

        var captor = ArgumentCaptor.forClass(Quarto.class);
        verify(quartos).save(captor.capture());
        assertEquals(101, captor.getValue().getNumero());
        assertEquals(QuartoStatus.DISPONIVEL, captor.getValue().getStatus());
        assertSame(tipo, captor.getValue().getTipo());
        assertEquals(10, resultado.getId());
    }

    @Test
    void rejeitaCadastroComTipoInexistenteSemSalvar() {
        when(tipos.findById(2)).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.criarQuarto(dto()));
        verify(quartos, never()).save(any());
    }

    @Test
    void rejeitaAtualizacaoComTipoInexistenteSemAlterarQuarto() {
        var quarto = new Quarto();
        quarto.setId(10);
        quarto.setNumero(100);
        quarto.setVersao(0L);

        when(quartos.findById(10)).thenReturn(Optional.of(quarto));
        when(tipos.findById(2)).thenReturn(Optional.empty());

        var dto = new AtualizarQuartoDTO(101, 2, 0L);

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.atualizarQuarto(10, dto));

        assertEquals(100, quarto.getNumero());
        verify(quartos, never()).saveAndFlush(any());
        verify(quartos, never()).save(any());
    }

    private QuartoDTO dto() {
        var dto = new QuartoDTO();
        dto.setNumero(101);
        dto.setIdTipo(2);
        return dto;
    }
}
