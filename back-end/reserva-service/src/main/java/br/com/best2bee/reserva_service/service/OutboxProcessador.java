package br.com.best2bee.reserva_service.service;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.best2bee.reserva_service.repository.OutboxEventoRepository;

@Service 
public class OutboxProcessador {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxProcessador.class);

    private final OutboxEventoRepository repository;
    private final ReservaEventoPublisher publisher;
    private final Clock clock;

    public OutboxProcessador
    (OutboxEventoRepository repository, ReservaEventoPublisher publisher, Clock clock){
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional 
    public Boolean processarProximo () {
        var pendente = repository.buscarProximoParaPublicar(clock.instant());

        if (pendente.isEmpty()){
            return false;
        }

        var evento = pendente.get();

        int tentativas = evento.getTentativas() == 
        Integer.MAX_VALUE ? Integer.MAX_VALUE : evento.getTentativas() + 1;

        evento.setTentativas(tentativas);

        try {
            publisher.publicar(evento);
        } catch (RuntimeException ex){
            if (Thread.currentThread().isInterrupted()){
                throw ex;
            }

            Long atrasoSegundos = Math.min(300L, 5L * (1L << Math.min(tentativas - 1, 6)));

            evento.setProximaTentativaEm(clock.instant().plusSeconds(atrasoSegundos));

            log.warn("Evento {} não publicado na tentativa {}. Nova tentativa em {} segundos",
                evento.getId(), tentativas, atrasoSegundos, ex);

                return true;
        }

        evento.setPublicadoEm(clock.instant());

        log.debug("Kafka confirmou o evento {} da reserva {}",
            evento.getId(), evento.getReservaId());

            return true;
    }
}
