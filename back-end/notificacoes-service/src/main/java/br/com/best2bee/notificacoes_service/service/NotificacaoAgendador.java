package br.com.best2bee.notificacoes_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hotel.notificacoes.envio-habilitado", havingValue = "true", matchIfMissing = false)
public class NotificacaoAgendador {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoAgendador.class);
    private final NotificacaoProcessador processador;

    public NotificacaoAgendador(NotificacaoProcessador processador) {

        this.processador = processador;
    }

    @Scheduled(initialDelayString = "${hotel.notificacoes.atraso-inicial-ms:10000}", fixedDelayString = "${hotel.notificacoes.intervalo-ms:2000}")
    public void executar() {
        for (int i = 0; i < 20; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                if (!processador.processarProximo()) {
                    return;
                }
            } catch (RuntimeException ex) {
                log.error("Falha no processamento daas notificações. \nUma nova execução ocorrerá no próximo ciclo",
                        ex);
                return;
            }
        }
    }
}
