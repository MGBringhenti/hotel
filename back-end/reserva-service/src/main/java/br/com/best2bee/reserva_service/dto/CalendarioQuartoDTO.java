package br.com.best2bee.reserva_service.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarioQuartoDTO(
        Integer idQuarto,
        boolean quartoAtivo,
        LocalDate dataInicio,
        LocalDate dataFinal,
        List<PeriodoOcupadoDTO> periodosOcupados
) {
}