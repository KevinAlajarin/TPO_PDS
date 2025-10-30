package com.scrim_pds.event;

import com.scrim_pds.model.Scrim;
import java.util.UUID; 

// Evento que se publica cuando un Scrim alcanza el cupo y pasa a Lobby Armado.

public record LobbyArmadoEvent(
    UUID scrimId,       
    String juego,       
    UUID organizadorId 

) implements DomainEvent {

    // Constructor adicional si queremos pasar el objeto Scrim completo
    public LobbyArmadoEvent(Scrim scrim) {
        this(scrim.getId(), scrim.getJuego(), scrim.getOrganizadorId());
    }
}
