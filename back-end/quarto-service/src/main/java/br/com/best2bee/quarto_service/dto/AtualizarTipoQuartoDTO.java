package br.com.best2bee.quarto_service.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record AtualizarTipoQuartoDTO(
    @NotBlank
    @Size(max = 50)
    String nome,

    @NotNull
    @Positive
    Integer capacidade,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    BigDecimal preco,

    @NotNull
    @PositiveOrZero
    Long versao
) {
}