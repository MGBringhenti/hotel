package br.com.best2bee.reserva_service.dto;

import java.time.LocalDate;

public record DisponibilidadeQuartoDTO(
        Integer idQuarto,
        LocalDate dataInicio,
        LocalDate dataFinal,
        boolean disponivel
) {
}