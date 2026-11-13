package br.com.best2bee.quarto_service.dto;

import br.com.best2bee.quarto_service.enums.QuartoStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AlterarStatusQuartoDTO(
    
    @NotNull
    QuartoStatus status,

    @NotNull
    @PositiveOrZero
    Long versao
) {
}