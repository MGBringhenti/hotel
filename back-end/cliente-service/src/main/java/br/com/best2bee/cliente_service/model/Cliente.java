package br.com.best2bee.cliente_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true)
    private String cpf;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "telefone", nullable = false, unique = true)
    private String telefone;

    @OneToOne
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private boolean ativo = true;

    @Version
    @Column (nullable = false)
    private Long versao;
}
