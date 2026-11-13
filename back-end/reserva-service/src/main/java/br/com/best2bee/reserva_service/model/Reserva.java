package br.com.best2bee.reserva_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.math.BigDecimal;

import br.com.best2bee.reserva_service.enums.Status;

@Entity 
@Table (name = "reserva")
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter 
public class Reserva {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "id", nullable = false)
    private Integer idReserva;

    @Column (name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column (name = "id_quarto", nullable = false)
    private Integer idQuarto;
    
    @Column (name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column (name = "data_final", nullable = false)
    private LocalDate dataFinal;

    @Column (name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated (EnumType.STRING)
    @Column (name = "status", nullable = false)
    private Status status;

    @Column (name = "preco_diario_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoDiarioAplicado;

    @Version
    @Column (nullable = false)
    private Long versao;
}
