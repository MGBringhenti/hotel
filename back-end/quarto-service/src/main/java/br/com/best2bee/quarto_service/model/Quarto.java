package br.com.best2bee.quarto_service.model;

import br.com.best2bee.quarto_service.enums.QuartoStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quartos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Quarto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "numero", nullable = false, unique = true)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuartoStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoQuarto tipo;

    @Version
    @Column(nullable = false)
    private Long versao;

}
