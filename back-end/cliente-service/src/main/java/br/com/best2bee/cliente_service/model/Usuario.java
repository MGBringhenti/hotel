package br.com.best2bee.cliente_service.model;

import br.com.best2bee.cliente_service.enums.Funcao;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "login", nullable = false, unique = true)
    private String login;

    @Column(name = "senha", nullable = false)
    private String senha;

    @Column(name = "funcao", nullable = false)
    @Enumerated(EnumType.STRING)
    private Funcao funcao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Version
    @Column (nullable = false)
    private Long versao;

    @Column(name = "email_recuperacao", length = 254, unique = true)
    private String emailRecuperacao;
}
