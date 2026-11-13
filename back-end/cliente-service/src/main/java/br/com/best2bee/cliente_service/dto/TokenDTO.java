package br.com.best2bee.cliente_service.dto;

public record TokenDTO (
    String accessToken,
    String tokenType,
    Long expiresIn
) {}