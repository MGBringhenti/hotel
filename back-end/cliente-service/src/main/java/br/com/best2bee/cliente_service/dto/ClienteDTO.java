package br.com.best2bee.cliente_service.dto;

import org.hibernate.validator.constraints.br.CPF;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ClienteDTO {
    
    @NotBlank(message = "O campo nome não pode estar em branco")
    @Size (max = 50, message = "O campo nome deve ter no máximo 50 caracteres")   
    private String nome;

    @NotBlank(message = "O campo cpf não pode estar em branco")
    @CPF(message = "O campo cpf deve ser um CPF válido")
    private String cpf;

    @NotBlank(message = "O campo email não pode estar em branco")
    @Size(max = 75, message = "O campo email deve ter no máximo 75 caracteres")
    @Email(message = "O campo email deve ser um endereço de email válido")
    private String email;

    @NotBlank(message = "O campo telefone não pode estar em branco")
    @Size(min = 10, max = 15, message = "O campo telefone deve ter entre 10 e 15 caracteres")
    private String telefone;
}
