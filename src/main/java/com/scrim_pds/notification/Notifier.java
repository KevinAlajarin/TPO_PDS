package com.scrim_pds.notification;

// Asumimos una clase 'Notification' simple por ahora
// import com.scrim_pds.model.Notification;

/**
 * Interfaz para cualquier canal de envío de notificaciones (Diagrama).
 * Simplificado para enviar directamente strings.
 */
public interface Notifier {

    // void send(Notification notification); // Versión del diagrama

    /**
     * Envía una notificación simple.
     * @param destinatario Dirección/token del destinatario.
     * @param asunto Título/Asunto.
     * @param cuerpo Mensaje.
     * @return true si fue exitoso/encolado.
     */
    boolean send(String destinatario, String asunto, String cuerpo);
}
