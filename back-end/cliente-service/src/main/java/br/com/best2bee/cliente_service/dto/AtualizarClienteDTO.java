package br.com.best2bee.cliente_service.dto;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AtualizarClienteDTO(

        @NotBlank 
        @Size(max = 50) 
        String nome,
        
        @NotBlank 
        @CPF 
        String cpf,

        @NotBlank 
        @Email 
        @Size(max = 75) String email,

        @NotBlank 
        @Size(min = 10, max = 15) String telefone,

        @NotNull(message = "A versão é obrigatória") 
        @PositiveOrZero Long versao) {

}
