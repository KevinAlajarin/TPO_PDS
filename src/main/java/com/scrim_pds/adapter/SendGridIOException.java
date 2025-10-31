package com.scrim_pds.adapter;

// Excepcion Runtime personalizada para envolver IOExceptions de SendGrid.

public class SendGridIOException extends RuntimeException {

    public SendGridIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
