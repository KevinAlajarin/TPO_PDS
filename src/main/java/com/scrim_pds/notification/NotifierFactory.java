package com.scrim_pds.notification;


    // Interfaz Abstract Factory (Diagrama).

public interface NotifierFactory {

    /**
     * Crea un notificador para el canal Email.
     * @return Una instancia de Notifier capaz de enviar emails.
     */
    Notifier createEmailNotifier();

    /**
     * Crea un notificador para el canal Push.
     * (No implementado).
     * @return Una instancia de Notifier capaz de enviar notificaciones push.
     */
    Notifier createPushNotifier();

    /**
     * Crea un notificador para el canal Discord.
     * (No implementado).
     * @return Una instancia de Notifier capaz de enviar webhooks de Discord.
     */
    Notifier createDiscordNotifier();
}
