package com.scrim_pds.notification;

    // Interfaz para cualquier canal de envío de notificaciones (Diagrama).

public interface Notifier {

    /**
     * Envia una notificacion simple.
     * @param destinatario Direccion/token del destinatario.
     * @param asunto Título/Asunto.
     * @param cuerpo Mensaje.
     * @return true si fue exitoso/encolado.
     */
    boolean send(String destinatario, String asunto, String cuerpo);
}
