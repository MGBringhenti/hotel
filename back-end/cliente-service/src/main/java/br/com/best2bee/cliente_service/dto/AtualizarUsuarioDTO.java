package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioDTO(

    @NotBlank(message = "O login não pode estar em branco")
    @Size (max = 75, message = "O login não pode passar de 75 caracteres")
    String login,

    @NotBlank(message = "O e-mail de recuperação é obrigatório")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
    String emailRecuperacao,

    @NotNull (message = "A versão é obrigatória")
    @PositiveOrZero (message = "A versão não pode ser negativa")
    Long versao
) {}
