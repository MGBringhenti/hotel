package br.com.best2bee.reserva_service.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import br.com.best2bee.reserva_service.dto.EventoKafkaDTO;
import br.com.best2bee.reserva_service.model.OutboxEvento;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ReservaEventoPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final String topico;

    public ReservaEventoPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper,
            @Value("${hotel.kafka.topico-reservas}") String topico) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.topico = topico;
    }

    public void publicar(OutboxEvento evento) {

        EventoKafkaDTO mensagem = new EventoKafkaDTO(
                evento.getId(),
                evento.getTipo(),
                evento.getCriadoEm(),
                jsonMapper.readTree(evento.getPayload()));

        String conteudo = jsonMapper.writeValueAsString(mensagem);
        String chave = evento.getReservaId().toString();

        try {
            kafkaTemplate.send(topico, chave, conteudo).get(35, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException("Thread interrompida ao publicar evento de reserva", e);

        } catch (ExecutionException ex) {

            throw new IllegalStateException(
                    "Kafka não confirmou a publicação do evento", ex.getCause());

        } catch (TimeoutException ex) {
            
            throw new IllegalStateException(
                    "Tempo de espera pela confirmação do Kafka excedido", ex);
        }
    }
}
