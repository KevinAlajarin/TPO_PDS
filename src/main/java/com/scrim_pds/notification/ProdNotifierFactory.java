package com.scrim_pds.notification;

import com.scrim_pds.adapter.EmailAdapter;
// import com.scrim_pds.adapter.FirebaseAdapter;
// import com.scrim_pds.adapter.DiscordAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary; 


// Implementación Concreta de la Factory para Producción (Diagrama: ProdNotifierFactory).

@Component
@Primary 
public class ProdNotifierFactory implements NotifierFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProdNotifierFactory.class);

    private final EmailAdapter emailAdapter;
    // private final PushAdapter pushAdapter;
    // private final DiscordAdapter discordAdapter;

    // Inyectamos los adapters que SÍ tenemos
    public ProdNotifierFactory(EmailAdapter emailAdapter /*, PushAdapter pushAdapter, DiscordAdapter discordAdapter */) {
        this.emailAdapter = emailAdapter;
        // this.pushAdapter = pushAdapter;
        // this.discordAdapter = discordAdapter
    }

    @Override
    public Notifier createEmailNotifier() {
        logger.debug("Creando instancia de EmailNotifier de producción (con SendGridAdapter)");
        return new EmailNotifier(emailAdapter);
    }

    @Override
    public Notifier createPushNotifier() {
        logger.warn("createPushNotifier() no está implementado en ProdNotifierFactory.");
        return (dest, asu, cue) -> {
            logger.warn("Intento de envío PUSH fallido: No implementado.");
            return false;
        };
    }

    @Override
    public Notifier createDiscordNotifier() {
        logger.warn("createDiscordNotifier() no está implementado en ProdNotifierFactory.");
        return (dest, asu, cue) -> {
            logger.warn("Intento de envío DISCORD fallido: No implementado.");
            return false;
        };
    }
}
