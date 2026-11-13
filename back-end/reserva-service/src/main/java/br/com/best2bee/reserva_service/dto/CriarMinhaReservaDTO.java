package br.com.best2bee.reserva_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarMinhaReservaDTO(
    
    @NotNull (message = "O ID do quarto é obrigatório") 
    @Positive (message = "O ID do quarto deve ser positivo")
    Integer idQuarto,

    @NotNull (message = "A data de entrada é obrigatória")
    LocalDate dataInicio,

    @NotNull (message = "A data de saída é obrigatória")
    LocalDate dataFinal
){}


