package br.com.best2bee.cliente_service.repository;

import br.com.best2bee.cliente_service.model.RecuperacaoSenha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface RecuperacaoSenhaRepository
        extends JpaRepository<RecuperacaoSenha, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT recuperacao
        FROM RecuperacaoSenha recuperacao
        WHERE recuperacao.tokenHash = :tokenHash
        """)
    Optional<RecuperacaoSenha> buscarParaUtilizacao(
            @Param("tokenHash") String tokenHash);

    boolean existsByUsuario_IdAndCriadoEmAfter(
        Integer usuarioId,
        Instant limite);
}