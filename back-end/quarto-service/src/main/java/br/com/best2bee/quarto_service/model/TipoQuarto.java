package br.com.best2bee.quarto_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tipoQuarto")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TipoQuarto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "capacidade", nullable = false)
    private Integer capacidade;

    @Column(name = "precodiario", precision = 12, scale = 2, nullable = false)
    private BigDecimal preco;

    @Version
    @Column(nullable = false)
    private Long versao;
}
