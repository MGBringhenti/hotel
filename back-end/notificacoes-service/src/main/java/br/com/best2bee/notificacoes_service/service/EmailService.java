package br.com.best2bee.notificacoes_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service 
public class EmailService {
    
    private final JavaMailSender sender;
    private final String remetente;

    public EmailService(JavaMailSender sender, @Value ("${hotel.notificacoes.remetente}") String remetente){

        this.sender = sender;
        this.remetente = remetente;
    }

    public void enviar(String destinatario, String assunto, String texto){
        if (destinatario == null || destinatario.isBlank()){
            throw new IllegalArgumentException("Destinatário do email é obrigatório");
        }

        if (assunto == null || assunto.isBlank()){
            throw new IllegalArgumentException("Assunto do email é obrigatório");
        }

        if (texto == null || texto.isBlank()){
            throw new IllegalArgumentException("Texto é obrigatório");
        }

        var mensagem  = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);

        sender.send(mensagem);
    }
}
