package br.com.best2bee.quarto_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.best2bee.quarto_service.model.Quarto;

public interface QuartoRepository extends JpaRepository<Quarto, Integer>{
    boolean existsByTipo_Id(Integer idTipo);
    
}

