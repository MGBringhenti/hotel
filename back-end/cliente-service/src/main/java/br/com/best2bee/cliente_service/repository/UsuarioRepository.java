package br.com.best2bee.cliente_service.repository;

import br.com.best2bee.cliente_service.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByLogin(String login);

    Optional<Usuario> findByEmailRecuperacao(String emailRecuperacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE usuario.emailRecuperacao = :email
            """)
    Optional<Usuario> buscarParaRecuperacao(@Param("email") String email);
}
