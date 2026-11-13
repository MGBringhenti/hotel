package br.com.best2bee.notificacoes_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservaEventoDTO(

    Integer versaoFormato,
    Integer idReserva,
    Long versaoReserva,
    Integer idCliente,
    Integer idQuarto,
    LocalDate dataInicio,
    LocalDate dataFinal,
    BigDecimal precoDiarioAplicado,
    BigDecimal valorTotal,
    String status
) {}
