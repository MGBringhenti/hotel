package br.com.best2bee.notificacoes_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import br.com.best2bee.notificacoes_service.model.Notificacao;

public interface NotificacaoRepository
        extends JpaRepository<Notificacao, UUID> {

    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = """
            INSERT INTO public.notificacao (
                evento_id,
                cliente_id
            )
            VALUES (
                :eventoId,
                :clienteId
            )
            """, nativeQuery = true)
    int inserir(
            @Param("eventoId") UUID eventoId,
            @Param("clienteId") Integer clienteId
    );

    @Query(value = """
        SELECT n.*
        FROM public.notificacao n
        WHERE n.enviado_em IS NULL
          AND n.descartado_em IS NULL
          AND n.proxima_tentativa_em <= :agora
        ORDER BY n.proxima_tentativa_em, n.criado_em, n.evento_id
        LIMIT 1
        FOR UPDATE OF n SKIP LOCKED
        """, nativeQuery = true)
Optional<Notificacao> buscarProximaParaEnviar(
        @Param("agora") Instant agora
);

}