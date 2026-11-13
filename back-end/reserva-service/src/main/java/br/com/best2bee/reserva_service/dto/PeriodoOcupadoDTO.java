package br.com.best2bee.reserva_service.dto;

import java.time.LocalDate;

public record PeriodoOcupadoDTO(
        LocalDate dataInicio,
        LocalDate dataFinal
) {
}