package com.scrim_pds.notification;

import com.scrim_pds.adapter.EmailAdapter;


// Implementación de Notifier para emails (Diagrama).

public class EmailNotifier implements Notifier {

    private final EmailAdapter emailAdapter;

    // Constructor
    public EmailNotifier(EmailAdapter emailAdapter) {
        this.emailAdapter = emailAdapter;
    }

    @Override
    public boolean send(String destinatario, String asunto, String cuerpo) {
        return emailAdapter.enviarEmail(destinatario, asunto, cuerpo);
    }
}

