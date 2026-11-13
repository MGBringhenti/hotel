package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacaoSenhaDTO(

        @NotBlank
        @Email
        @Size(max = 254)
        String emailRecuperacao

) {}