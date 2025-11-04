package com.scrim_pds.dto;

import com.scrim_pds.model.Estadistica;
import com.scrim_pds.model.User;

import java.util.UUID;

// Nuevo DTO para enviar al Frontend.
// Combina los datos de la Estadística con el nombre del usuario.
public class EstadisticaResponse {

    private UUID id;
    private UUID scrimId;
    private UUID usuarioId;
    private String username; // <-- El dato clave que faltaba
    private boolean mvp;
    private int kills;
    private int deaths;
    private int assists;
    private String observaciones;

    // Constructor que combina ambos modelos
    public EstadisticaResponse(Estadistica estadistica, User user) {
        this.id = estadistica.getId();
        this.scrimId = estadistica.getScrimId();
        this.usuarioId = estadistica.getUsuarioId();
        this.mvp = estadistica.isMvp();
        this.kills = estadistica.getKills();
        this.deaths = estadistica.getDeaths();
        this.assists = estadistica.getAssists();
        this.observaciones = estadistica.getObservaciones();

        // Si el usuario no es nulo, seteamos el username
        if (user != null) {
            this.username = user.getUsername();
        } else {
            this.username = "Usuario Desconocido"; // Fallback
        }
    }

    // Getters (necesarios para Jackson)
    public UUID getId() { return id; }
    public UUID getScrimId() { return scrimId; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getUsername() { return username; }
    public boolean isMvp() { return mvp; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public String getObservaciones() { return observaciones; }
}
