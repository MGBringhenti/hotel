package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VincularUsuarioDTO(
        @NotNull(message = "O ID do usuário é obrigatório")
        @Positive(message = "O ID do usuário deve ser positivo")
        Integer usuarioId
) {}