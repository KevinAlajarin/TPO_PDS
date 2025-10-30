package com.scrim_pds.notification;

import com.scrim_pds.adapter.EmailAdapter;
// import org.springframework.beans.factory.annotation.Qualifier; // <-- BORRADO
// import org.springframework.stereotype.Component; // <-- BORRADO

/**
 * Implementación de Notifier para emails (Diagrama).
 * Esta clase YA NO es un @Component. Será instanciada por la NotifierFactory.
 */
// @Component // <-- BORRADO
// @Qualifier("emailNotifier") // <-- BORRADO
public class EmailNotifier implements Notifier {

    private final EmailAdapter emailAdapter;

    // El constructor sigue igual, la Factory lo llamará
    public EmailNotifier(EmailAdapter emailAdapter) {
        this.emailAdapter = emailAdapter;
    }

    @Override
    public boolean send(String destinatario, String asunto, String cuerpo) {
        // Delega el envío al adapter
        return emailAdapter.enviarEmail(destinatario, asunto, cuerpo);
    }
}

