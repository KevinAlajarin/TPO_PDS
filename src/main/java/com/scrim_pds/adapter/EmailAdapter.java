package com.scrim_pds.adapter;

// Interfaz Adapter para el servicio de envio de correo.

public interface EmailAdapter {

    /**
     * Envía un correo electrónico.
     * @param destinatario Email del receptor.
     * @param asunto Asunto del correo.
     * @param cuerpo Contenido HTML o texto plano del correo.
     * @return true si el envío fue exitoso (o encolado), false si falló inmediatamente.
     */
    boolean enviarEmail(String destinatario, String asunto, String cuerpo);
}
