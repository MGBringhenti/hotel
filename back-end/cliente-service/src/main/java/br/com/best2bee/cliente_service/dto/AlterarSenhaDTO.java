package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaDTO(

    @NotBlank (message = "Informe a senha atual")
    @Size (max =  50, message = "A senha não pode passar de 50 caracteres")
    String senhaAtual,

    @NotBlank (message = "Informe a nova senha")
    @Size (min = 8, max = 50, message = "A senha deve ter entre 8 e 50 caracteres")
    String senhaNova
) {}

