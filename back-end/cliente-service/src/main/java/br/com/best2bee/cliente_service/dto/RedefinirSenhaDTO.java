package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaDTO(

        @NotBlank
        @Size(max = 200)
        String token,

        @NotBlank
        @Size(min = 8, max = 50)
        String senhaNova

) {}