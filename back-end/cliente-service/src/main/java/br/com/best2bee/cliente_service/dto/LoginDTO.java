package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO (

    @NotBlank String login,
    @NotBlank String senha
) {}