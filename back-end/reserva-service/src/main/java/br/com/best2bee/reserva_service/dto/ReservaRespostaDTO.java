package br.com.best2bee.reserva_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.best2bee.reserva_service.enums.Status;
import br.com.best2bee.reserva_service.model.Reserva;

public record ReservaRespostaDTO(

        Integer idReserva,
        Integer idCliente,
        Integer idQuarto,
        LocalDate dataInicio,
        LocalDate dataFinal,
        BigDecimal precoDiarioAplicado,
        BigDecimal valorTotal,
        Status status,
        Long versao) {

    public static ReservaRespostaDTO from(Reserva reserva) {
        return new ReservaRespostaDTO(reserva.getIdReserva(),
                reserva.getIdCliente(),
                reserva.getIdQuarto(),
                reserva.getDataInicio(),
                reserva.getDataFinal(),
                reserva.getPrecoDiarioAplicado(),
                reserva.getValorTotal(),
                reserva.getStatus(),
                reserva.getVersao());
    }
}
