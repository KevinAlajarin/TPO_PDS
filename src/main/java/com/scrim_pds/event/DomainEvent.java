package com.scrim_pds.event;

import java.time.LocalDateTime;

/**
 * Interfaz marcadora para todos los eventos de dominio (Diagrama).
 */
public interface DomainEvent {
    // Podría tener métodos comunes, como obtener timestamp
    default LocalDateTime occurredOn() {
        return LocalDateTime.now();
    }
}
