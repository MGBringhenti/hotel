package br.com.best2bee.reserva_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import br.com.best2bee.reserva_service.model.Reserva;
import br.com.best2bee.reserva_service.enums.Status;
import br.com.best2bee.reserva_service.dto.PeriodoOcupadoDTO;


public interface ReservaRepository
        extends JpaRepository<Reserva, Integer> {

    Page<Reserva> findByIdCliente(
            Integer idCliente,
            Pageable pageable);

    @Query("""
        select new br.com.best2bee.reserva_service.dto.PeriodoOcupadoDTO(
            r.dataInicio,
            r.dataFinal
        )
        from Reserva r
        where r.idQuarto = :quartoId
          and r.status in :statusBloqueantes
          and r.dataInicio < :fim
          and r.dataFinal > :inicio
        order by r.dataInicio, r.dataFinal
        """)
List<PeriodoOcupadoDTO> buscarPeriodosOcupados(
        @Param("quartoId") Integer quartoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim,
        @Param("statusBloqueantes") Collection<Status> statusBloqueantes
);

    @Query ("""
        select count(r)
        from Reserva r
        where r.idQuarto = :quartoId
        and r.status in :statusBloqueantes
        and r.dataInicio < :fim
        and r.dataFinal > :inicio
        and (:ignorarId is null or r.idReserva <> :ignorarId)
    """)
    Long contarConflitos(
        @Param("quartoId") Integer quartoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim,
        @Param("statusBloqueantes") Collection<Status> statusBloqueantes,
        @Param("ignorarId") Integer ignorarId   
    );
}