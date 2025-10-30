package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.util.UUID;

// Evento que se publica cuando un Scrim pasa al estado FINALIZADO.

public record ScrimFinalizadoEvent(
    UUID scrimId,
    String juego,
    UUID organizadorId
) implements DomainEvent {

     public ScrimFinalizadoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getOrganizadorId());
    }
}
