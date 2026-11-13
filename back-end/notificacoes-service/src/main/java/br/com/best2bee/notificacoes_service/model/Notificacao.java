package br.com.best2bee.notificacoes_service.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificacao", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class Notificacao {

    @Id
    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(name = "descartado_em")
    private Instant descartadoEm;

    @Column(name = "motivo_descarte", length = 100)
    private String motivoDescarte;
}