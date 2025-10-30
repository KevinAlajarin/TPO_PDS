package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;

// Evento que se publica cuando se crea un nuevo Scrim. 

public record ScrimCreatedEvent(Scrim scrim) implements DomainEvent {
}

