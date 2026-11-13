package br.com.best2bee.cliente_service.repository;

import br.com.best2bee.cliente_service.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByUsuario_Id(Integer usuarioId);
    boolean existsByUsuario_IdAndIdNot(Integer usuarioId, Integer clienteId);
}
