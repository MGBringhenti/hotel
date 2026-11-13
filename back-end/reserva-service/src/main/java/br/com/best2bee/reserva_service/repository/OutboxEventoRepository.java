package br.com.best2bee.reserva_service.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.best2bee.reserva_service.model.OutboxEvento;

public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, UUID> {

     @Query(value = """
            SELECT e.*
            FROM public.outbox_evento e
            WHERE e.publicado_em IS NULL
              AND e.proxima_tentativa_em <= :agora
              AND NOT EXISTS (
                  SELECT 1
                  FROM public.outbox_evento anterior
                  WHERE anterior.reserva_id = e.reserva_id
                    AND anterior.reserva_versao < e.reserva_versao
                    AND anterior.publicado_em IS NULL
              )
            ORDER BY e.proxima_tentativa_em, e.criado_em, e.id
            LIMIT 1
            FOR UPDATE OF e SKIP LOCKED
            """, nativeQuery = true)    

    Optional<OutboxEvento> buscarProximoParaPublicar(@Param("agora") Instant agora);
}