package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.util.UUID;

/**
 * Evento que se publica cuando un Scrim pasa al estado CANCELADO.
 */
public record ScrimCanceladoEvent(
    UUID scrimId,
    String juego,
    UUID organizadorId
) implements DomainEvent {

     public ScrimCanceladoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getOrganizadorId());
    }
}
