package br.com.best2bee.reserva_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record AlterarReservaDTO(
    
        @NotNull(message = "O ID do cliente é obrigatório")
        @Positive(message = "O ID do cliente deve ser positivo")
        Integer idCliente,

        @NotNull(message = "O ID do quarto é obrigatório")
        @Positive(message = "O ID do quarto deve ser positivo")
        Integer idQuarto,

        @NotNull(message = "A data de entrada é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "A data de saída é obrigatória")
        LocalDate dataFinal,

        @NotNull(message = "A versão da reserva é obrigatória")
        @PositiveOrZero(message = "A versão não pode ser negativa")
        Long versao
){}
