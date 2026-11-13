package br.com.best2bee.cliente_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recuperacao_senha")
@Getter
@Setter
@NoArgsConstructor
public class RecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "usuario_versao", nullable = false)
    private Long usuarioVersao;

    @Column(name = "token_hash", nullable = false,
            unique = true, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "utilizado_em")
    private Instant utilizadoEm;
}