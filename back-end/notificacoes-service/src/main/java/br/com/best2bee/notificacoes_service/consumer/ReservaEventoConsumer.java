package br.com.best2bee.notificacoes_service.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.best2bee.notificacoes_service.dto.EventoKafkaDTO;
import br.com.best2bee.notificacoes_service.service.EventoRecebidoService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ReservaEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaEventoConsumer.class);

    private final JsonMapper jsonMapper;
    private final EventoRecebidoService eventoRecebidoService;

    public ReservaEventoConsumer(JsonMapper jsonMapper, EventoRecebidoService eventoRecebidoService) {

        this.jsonMapper = jsonMapper;
        this.eventoRecebidoService = eventoRecebidoService;
    }

    @KafkaListener(topics = "${hotel.kafka.topico-reservas}")
    public void receber(ConsumerRecord<String, String> registro) {
        String conteudo = registro.value();

        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("Conteúdo do evento obrigatório");
        }

        EventoKafkaDTO evento;

        try {
            evento = jsonMapper.readValue(conteudo, EventoKafkaDTO.class);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Mensagem não corresponde ao formato esperado", ex);
        }

        if (evento != null && evento.dados() != null
                && evento.dados().idReserva() != null
                && !evento.dados().idReserva().toString().equals(registro.key())) {

            throw new IllegalArgumentException("Chave kafka diferente do identificador da reserva");
        }

        boolean novo = eventoRecebidoService.registrar(evento);

        if (novo) {
            log.info("Evento {} recebido: tipo = {}, reserva = {}",
                    evento.eventoId(), evento.tipo(), evento.dados().idReserva());
        } else {
            log.debug("Evento {} já recebido, gravação não repetida", evento.eventoId());
        }
    }

}
