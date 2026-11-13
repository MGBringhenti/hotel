package br.com.best2bee.reserva_service.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "outbox_evento")
@Getter 
@Setter 
@NoArgsConstructor 
public class OutboxEvento {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "reserva_id", nullable = false)
    private Integer reservaId;

    @Column (name = "reserva_versao", nullable = false)
    private Long reservaVersao;

    @Column (nullable = false, length = 50)
    private String tipo;

    @JdbcTypeCode (SqlTypes.JSON)
    @Column (nullable = false, columnDefinition = "jsonb")
    private String payload;

     @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;
}
