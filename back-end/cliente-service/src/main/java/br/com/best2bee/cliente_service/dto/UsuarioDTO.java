package br.com.best2bee.cliente_service.dto;

import br.com.best2bee.cliente_service.enums.Funcao;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {

    @NotBlank(message = "O e-mail de recuperação é obrigatório")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
    private String emailRecuperacao;
    
    @NotBlank (message = "O campo login não pode estar em branco")
    @Size (max = 75, message = "O campo login deve ter no máximo 75 caracteres")
    private String login;

    @NotBlank (message = "O campo senha não pode estar em branco")
    @Size (min = 8, max = 50, message = "O campo senha deve ter entre 8 e 50 caracteres")
    private String senha;

    @NotNull (message = "O campo função não pode ser estar em branco, e deve ser ADMIN, CLIENTE ou RECEPCIONISTA")
    private Funcao funcao;

}
