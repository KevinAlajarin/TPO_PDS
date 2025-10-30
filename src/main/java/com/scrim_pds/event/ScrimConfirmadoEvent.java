package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.time.LocalDateTime; 
import java.util.UUID;

// Evento que se publica cuando todos los participantes han confirmado y el Scrim pasa al estado CONFIRMADO.

public record ScrimConfirmadoEvent(
    UUID scrimId,
    String juego,
    LocalDateTime fechaHora, 
    UUID organizadorId
) implements DomainEvent {

    // Constructor adicional para crear desde el objeto Scrim
    public ScrimConfirmadoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getFechaHora(), scrim.getOrganizadorId());
    }
}
