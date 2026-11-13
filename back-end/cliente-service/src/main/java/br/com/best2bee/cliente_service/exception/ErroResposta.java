package br.com.best2bee.cliente_service.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ErroResposta {
    private LocalDateTime timestamp;
    private Integer status;
    private String erro;
    private String mensagem;

    public ErroResposta(Integer status, String erro, String mensagem) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
    }
}
