package com.scrim_pds.event;

import java.time.LocalDateTime;

// Interfaz marcadora para todos los eventos de dominio (Diagrama).

public interface DomainEvent {
    default LocalDateTime occurredOn() {
        return LocalDateTime.now();
    }
}
