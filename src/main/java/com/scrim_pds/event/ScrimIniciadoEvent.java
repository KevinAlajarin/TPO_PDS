package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.time.LocalDateTime;
import java.util.UUID;

// Evento que se publica cuando un Scrim pasa al estado EN_JUEGO.

public record ScrimIniciadoEvent(
    UUID scrimId,
    String juego,
    LocalDateTime fechaHora,
    UUID organizadorId
) implements DomainEvent {

    public ScrimIniciadoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getFechaHora(), scrim.getOrganizadorId());
    }
}
