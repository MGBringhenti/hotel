package br.com.best2bee.quarto_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarQuartoDTO(

    @NotNull
    @Positive
    Integer numero,

    @NotNull
    @Positive
    Integer idTipo,

    @NotNull(message = "A versão é obrigatória")
    @PositiveOrZero
    Long versao
) {}