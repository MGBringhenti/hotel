package br.com.best2bee.notificacoes_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration 
public class KafkaConsumerConfig {
    
    @Bean 
    public DefaultErrorHandler kafkaErrorHandler
    (KafkaTemplate<String, String> kafkaTemplate, 
        @Value("${hotel.kafka.topico-reservas-dlt}") String topicoDlt){
            var recuperador =  new DeadLetterPublishingRecoverer
            (kafkaTemplate, (registro, erro) -> new TopicPartition(topicoDlt, registro.partition()));

            recuperador.setFailIfSendResultIsError(true);

            var handler = new DefaultErrorHandler(recuperador, new FixedBackOff(2000L, 3L));

            handler.addNotRetryableExceptions(IllegalArgumentException.class);

            return handler;
        }
}
