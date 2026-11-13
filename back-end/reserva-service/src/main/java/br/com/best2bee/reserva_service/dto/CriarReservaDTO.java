package br.com.best2bee.reserva_service.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record CriarReservaDTO(    
    
    @NotNull (message = "ID do cliente é obrigatório ")
    @Positive (message = "ID do cliente deve ser maior que zero ")
    Integer idCliente,

    @NotNull(message = "ID do quarto é obrigatório ")
    @Positive(message = "ID do quarto deve ser positivo")
    Integer idQuarto,

    @NotNull(message = "A data de entrada é obrigatória")
    LocalDate dataInicio,

    @NotNull(message = "A data de saída é obrigatória")
    LocalDate dataFinal
    
) {}


