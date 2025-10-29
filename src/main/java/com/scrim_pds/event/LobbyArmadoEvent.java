package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.util.UUID; // Import UUID

/**
 * Evento que se publica cuando un Scrim alcanza el cupo y pasa a Lobby Armado.
 */
public record LobbyArmadoEvent(
    UUID scrimId,       // ID del scrim
    String juego,       // Nombre del juego para el email
    UUID organizadorId // ID del organizador para notificarle
    // Podríamos añadir la lista de IDs de postulantes aquí,
    // pero es más seguro buscarla en el suscriptor
) implements DomainEvent {

    // Constructor canónico generado por el record

    // Constructor adicional si queremos pasar el objeto Scrim completo
    public LobbyArmadoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getOrganizadorId());
    }
}
