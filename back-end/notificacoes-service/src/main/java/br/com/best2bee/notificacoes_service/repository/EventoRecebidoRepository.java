package br.com.best2bee.notificacoes_service.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.best2bee.notificacoes_service.model.EventoRecebido;

public interface EventoRecebidoRepository
        extends JpaRepository<EventoRecebido, UUID> {

    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = """
            INSERT INTO public.evento_recebido (
                evento_id,
                tipo,
                reserva_id,
                reserva_versao,
                payload,
                ocorrido_em
            )
            VALUES (
                :eventoId,
                :tipo,
                :reservaId,
                :reservaVersao,
                CAST(:payload AS jsonb),
                :ocorridoEm
            )
            ON CONFLICT (evento_id) DO NOTHING
            """, nativeQuery = true)
    int inserirSeAusente(
            @Param("eventoId") UUID eventoId,
            @Param("tipo") String tipo,
            @Param("reservaId") Integer reservaId,
            @Param("reservaVersao") Long reservaVersao,
            @Param("payload") String payload,
            @Param("ocorridoEm") Instant ocorridoEm
    );

    boolean existsByReservaIdAndReservaVersaoGreaterThan(
        Integer reservaId,
        Long reservaVersao
);

}