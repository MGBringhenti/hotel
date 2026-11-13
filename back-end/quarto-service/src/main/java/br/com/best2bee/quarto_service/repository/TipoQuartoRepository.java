package br.com.best2bee.quarto_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.best2bee.quarto_service.model.TipoQuarto;

public interface TipoQuartoRepository extends JpaRepository<TipoQuarto, Integer> {
    
}
