package br.com.best2bee.notificacoes_service.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evento_recebido", schema = "public")
@Getter
@NoArgsConstructor
public class EventoRecebido {

    @Id
    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "reserva_id", nullable = false)
    private Integer reservaId;

    @Column(name = "reserva_versao", nullable = false)
    private Long reservaVersao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;
}