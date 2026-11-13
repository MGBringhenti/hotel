package br.com.best2bee.cliente_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailRecuperacaoService {
    
    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailRecuperacaoService(JavaMailSender mailSender, @Value ("${hotel.email.remetente}") String remetente){
        this.mailSender = mailSender;
        this.remetente = remetente;       
    }

    public void enviarToken(String destinatario, String token){
        SimpleMailMessage mensagem = new SimpleMailMessage();

        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Hotel - Recuperação de senha");

        mensagem.setText("""
                Recebemos uma solicitação de recuperação de senha.

                Utilize o token abaixo para cadastrar uma nova senha:

                %s

                Este token é válido por 30 minutos e pode ser utilizado
                uma única vez.

                Se você não solicitou a recuperação, ignore esta mensagem.
                Sua senha permanecerá a mesma.
                """.formatted(token));

            mailSender.send(mensagem);
    }
}
