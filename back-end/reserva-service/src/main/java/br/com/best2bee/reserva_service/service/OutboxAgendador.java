package br.com.best2bee.reserva_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hotel.outbox.habilitado", havingValue = "true", matchIfMissing = true)
public class OutboxAgendador {

    private static final Logger log = LoggerFactory.getLogger(OutboxAgendador.class);

    private final OutboxProcessador processador;

    public OutboxAgendador(OutboxProcessador processador) {
        this.processador = processador;
    }

    @Scheduled(initialDelayString = "${hotel.outbox.atraso-inicial-ms:10000}", fixedDelayString = "${hotel.outbox.intervalo-ms:2000}")
    public void executar() {
        for (int i = 0; i < 20; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                if (!processador.processarProximo()){
                    return;
                }
            } catch (RuntimeException ex) {
                log.error("Falha ao processar Outbox, processamento ocorrerá no próximo ciclo", ex);
                return;
            }
        }
    }
}
