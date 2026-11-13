package br.com.best2bee.reserva_service.dto;

public record QuartoConsultaDTO (
    Integer id,
    String status,
    TipoQuartoConsultaDTO tipo
) {}