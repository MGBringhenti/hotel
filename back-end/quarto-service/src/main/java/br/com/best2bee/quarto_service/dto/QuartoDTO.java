package br.com.best2bee.quarto_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuartoDTO {

    @NotNull(message = "O número do quarto não pode ser nulo")
    @Min(value = 1, message = "O número do quarto deve ser maior que zero")
    private Integer numero;

    @NotNull(message = "O ID do tipo de quarto não pode ser nulo")
    @Min(value = 1, message = "O ID do tipo de quarto deve ser maior que zero")
    private Integer idTipo;
}
