package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginServicoDTO(
        @NotBlank
        @Size(max = 100)
        String clientId,

        @NotBlank
        @Size(min = 32, max = 72)
        String clientSecret
) {
}