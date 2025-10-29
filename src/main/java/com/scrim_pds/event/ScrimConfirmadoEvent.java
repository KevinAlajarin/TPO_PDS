package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.time.LocalDateTime; // Import LocalDateTime
import java.util.UUID;

/**
 * Evento que se publica cuando todos los participantes han confirmado
 * y el Scrim pasa al estado CONFIRMADO.
 */
public record ScrimConfirmadoEvent(
    UUID scrimId,
    String juego,
    LocalDateTime fechaHora, // Añadir fecha/hora para el email
    UUID organizadorId
) implements DomainEvent {

    // Constructor canónico generado por el record

    // Constructor adicional para crear desde el objeto Scrim
    public ScrimConfirmadoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getFechaHora(), scrim.getOrganizadorId());
    }
}
