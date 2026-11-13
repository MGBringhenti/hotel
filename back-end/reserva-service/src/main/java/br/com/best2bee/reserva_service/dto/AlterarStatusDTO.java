package br.com.best2bee.reserva_service.dto;

import br.com.best2bee.reserva_service.enums.Status;
import jakarta.validation.constraints.*;

public record AlterarStatusDTO(    

    @NotNull (message = "Status da reserva é obrigatório ")
    Status status,

    @NotNull (message = "A versão da reserva é obrigatória ")
    @PositiveOrZero (message = "A versão não pode ser negativa ")
    Long versao

) {}
