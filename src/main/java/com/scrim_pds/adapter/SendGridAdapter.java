package com.scrim_pds.adapter;

import com.sendgrid.*; 
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;


// Implementación concreta del EmailAdapter usando SendGrid.

@Component 
public class SendGridAdapter implements EmailAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SendGridAdapter.class);

    private final SendGrid sendGridClient;
    private final String fromEmail;

    // Inyectamos los valores desde application.properties
    public SendGridAdapter(@Value("${sendgrid.api-key}") String apiKey,
                           @Value("${sendgrid.from-email}") String fromEmail) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("TU_SENDGRID_API_KEY_AQUI")) {
             logger.warn("!!! SendGrid API Key no configurada o es el valor por defecto. Los emails no se enviarán. !!!");
             this.sendGridClient = null; // No inicializar si no hay key
        } else {
             this.sendGridClient = new SendGrid(apiKey);
        }
        this.fromEmail = fromEmail;
    }

    @Override
    public boolean enviarEmail(String destinatario, String asunto, String cuerpo) {
        // Si el cliente no se inicializo (falta API key), no intentar enviar.
        if (sendGridClient == null) {
            logger.warn("SendGrid client no inicializado (API Key faltante?). Email a {} no enviado.", destinatario);
            return false;
        }

        Email from = new Email(this.fromEmail);
        Email to = new Email(destinatario);
        Content content = new Content("text/plain", cuerpo);
        Mail mail = new Mail(from, asunto, to, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            logger.info("Enviando email a {} con asunto '{}' via SendGrid...", destinatario, asunto);
            Response response = sendGridClient.api(request);

            // SendGrid devuelve 2xx si el email fue aceptado para envio
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email a {} aceptado por SendGrid (StatusCode: {})", destinatario, response.getStatusCode());
                return true;
            } else {
                logger.error("Error al enviar email a {} via SendGrid. StatusCode: {}, Body: {}",
                        destinatario, response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (IOException ex) {
            logger.error("Error de IO al intentar enviar email a {} via SendGrid: {}", destinatario, ex.getMessage());
            return false;
        }
    }
}
