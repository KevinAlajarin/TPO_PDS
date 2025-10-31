package com.scrim_pds.exception;

/**
 * Excepción lanzada cuando un usuario intenta dejar feedback
 * de forma inválida (ej. scrim no finalizado, no es participante, etc.).
 * Se traduce a un HTTP 403 Forbidden.
 */
public class FeedbackNotAllowedException extends RuntimeException {
    public FeedbackNotAllowedException(String message) {
        super(message);
    }
}
