package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AlterarSituacaoClienteDTO(

    @NotNull (message = "Deve informar se cliente está ativo")
    Boolean ativo,

    @NotNull (message = "A versão do cliente é obrigatória")
    @PositiveOrZero (message = "A versão deve ser positiva")
    Long versao
) {}
