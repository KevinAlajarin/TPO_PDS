package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;

/**
 * Evento que se publica cuando se crea un nuevo Scrim (Diagrama).
 * Usamos un Record de Java 17 para simplicidad.
 */
public record ScrimCreatedEvent(Scrim scrim) implements DomainEvent {
    // El record genera automáticamente constructor, getters, equals, hashCode, toString
}

// Si no usaras Records (Java < 17), sería una clase normal:
/*
public class ScrimCreatedEvent implements DomainEvent {
    private final Scrim scrim;

    public ScrimCreatedEvent(Scrim scrim) {
        this.scrim = Objects.requireNonNull(scrim);
    }

    public Scrim getScrim() {
        return scrim;
    }
    // equals, hashCode, toString...
}
*/
