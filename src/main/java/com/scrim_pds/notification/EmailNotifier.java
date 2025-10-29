package com.scrim_pds.notification;

import com.scrim_pds.adapter.EmailAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Implementación de Notifier para emails (Diagrama).
 */
@Component
@Qualifier("emailNotifier") // Para poder inyectar específicamente este Notifier
public class EmailNotifier implements Notifier {

    private final EmailAdapter emailAdapter;

    // Inyectamos la implementación concreta de EmailAdapter (SendGridAdapter)
    public EmailNotifier(EmailAdapter emailAdapter) {
        this.emailAdapter = emailAdapter;
    }

    @Override
    public boolean send(String destinatario, String asunto, String cuerpo) {
        // Delega el envío al adapter
        return emailAdapter.enviarEmail(destinatario, asunto, cuerpo);
    }
}
