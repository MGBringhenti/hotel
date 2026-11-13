package br.com.best2bee.quarto_service.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter 
@Setter
public class TipoQuartoDTO {
    
    @NotBlank (message = "O nome do tipo de quarto não pode ser vazio")
    @Size (max = 50, message = "O nome do tipo de quarto deve ter no máximo 50 caracteres")
    private String nome;

    @NotNull (message = "A capacidade do tipo de quarto não pode ser nula")
    @Min (value = 1, message = "A capacidade do tipo de quarto deve ser maior que zero")
    private Integer capacidade;

    @NotNull (message = "O preço do tipo de quarto não pode ser nulo")
    @DecimalMin (value = "0.0", inclusive = false, message = "O preço do tipo de quarto deve ser maior que zero")
    @Digits (integer = 10, fraction = 2, message = "O número pode ter até 10 digitos inteiros e dois decimais")
    private BigDecimal preco;
}
