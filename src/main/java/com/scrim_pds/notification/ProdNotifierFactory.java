package com.scrim_pds.notification;

import com.scrim_pds.adapter.EmailAdapter;
// Importar otros adapters si los tuviéramos
// import com.scrim_pds.adapter.FirebaseAdapter;
// import com.scrim_pds.adapter.DiscordAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary; // Para marcarla como la factory por defecto

/**
 * Implementación Concreta de la Factory para Producción (Diagrama: ProdNotifierFactory).
 * Crea Notifiers reales que usan adapters reales (SendGrid, Firebase, etc.).
 */
@Component
@Primary // Si tuviéramos TestNotifierFactory, esta sería la de por defecto
public class ProdNotifierFactory implements NotifierFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProdNotifierFactory.class);

    // La factory necesita los adapters para construir los notifiers
    private final EmailAdapter emailAdapter;
    // private final PushAdapter pushAdapter;
    // private final DiscordAdapter discordAdapter;

    // Inyectamos los adapters que SÍ tenemos
    public ProdNotifierFactory(EmailAdapter emailAdapter /*, PushAdapter pushAdapter, etc. */) {
        this.emailAdapter = emailAdapter;
        // this.pushAdapter = pushAdapter;
    }

    @Override
    public Notifier createEmailNotifier() {
        logger.debug("Creando instancia de EmailNotifier de producción (con SendGridAdapter)");
        // El EmailNotifier necesita el EmailAdapter para funcionar
        return new EmailNotifier(emailAdapter);
    }

    @Override
    public Notifier createPushNotifier() {
        logger.warn("createPushNotifier() no está implementado en ProdNotifierFactory.");
        // Devolver un "Null Notifier" que no hace nada para evitar NullPointerExceptions
        return (dest, asu, cue) -> {
            logger.warn("Intento de envío PUSH fallido: No implementado.");
            return false;
        };
        // O: throw new UnsupportedOperationException("Push notifications no implementadas");
    }

    @Override
    public Notifier createDiscordNotifier() {
        logger.warn("createDiscordNotifier() no está implementado en ProdNotifierFactory.");
        // Devolver un "Null Notifier"
        return (dest, asu, cue) -> {
            logger.warn("Intento de envío DISCORD fallido: No implementado.");
            return false;
        };
    }
}
