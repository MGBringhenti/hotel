package br.com.best2bee.reserva_service.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.best2bee.reserva_service.dto.ReservaEventoDTO;
import br.com.best2bee.reserva_service.enums.TipoEventoReserva;
import br.com.best2bee.reserva_service.model.OutboxEvento;
import br.com.best2bee.reserva_service.model.Reserva;
import br.com.best2bee.reserva_service.repository.OutboxEventoRepository;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OutboxService {
    
    private final OutboxEventoRepository outboxEventoRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxService(OutboxEventoRepository outboxEventoRepository, JsonMapper jsonMapper, Clock clock){
        this.outboxEventoRepository = outboxEventoRepository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Transactional (propagation = Propagation.MANDATORY)
    public void registrar(Reserva reserva, TipoEventoReserva tipo){
        Objects.requireNonNull(reserva, "Reserva é obrigatória");
        Objects.requireNonNull(tipo, "Tipo de evento é obrigatório");

        if (reserva.getIdReserva() == null || reserva.getVersao() == null){
            throw new IllegalStateException("Reserva deve ter id e versão");
        }

        ReservaEventoDTO dados = new ReservaEventoDTO(
            1,
            reserva.getIdReserva(),
            reserva.getVersao(),
            reserva.getIdCliente(),
            reserva.getIdQuarto(),
            reserva.getDataInicio(),
            reserva.getDataFinal(),
            reserva.getPrecoDiarioAplicado(),
            reserva.getValorTotal(),
            reserva.getStatus().name()
        );
    
    Instant agora = clock.instant();

    OutboxEvento evento = new OutboxEvento();
        evento.setReservaId(reserva.getIdReserva());
        evento.setReservaVersao(reserva.getVersao());
        evento.setTipo(tipo.name());
        evento.setPayload(jsonMapper.writeValueAsString(dados));
        evento.setCriadoEm(agora);
        evento.setProximaTentativaEm(agora);
        evento.setTentativas(0);

        outboxEventoRepository.save(evento);
    }
}
