package br.com.best2bee.cliente_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecuperacaoSenhaListener {

    private static final Logger log =
            LoggerFactory.getLogger(RecuperacaoSenhaListener.class);

    private final EmailRecuperacaoService emailService;

    public RecuperacaoSenhaListener(
            EmailRecuperacaoService emailService) {

        this.emailService = emailService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void enviar(RecuperacaoSenhaSolicitada evento) {
        try {
            emailService.enviarToken(
                    evento.destinatario(),
                    evento.token()
            );
        } catch (MailException ex) {
            log.error(
                    "Falha no envio do e-mail de recuperação de senha"
            );
        }
    }
}