package com.scrim_pds.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class EstadisticaRequest {

    @NotNull
    private UUID usuarioId; // ID del jugador 

    private boolean mvp = false; // Valor por defecto

    @NotNull
    private Integer kills;

    @NotNull
    private Integer deaths;

    @NotNull
    private Integer assists;
    
    private String observaciones;

    // Getters y Setters
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public boolean isMvp() { return mvp; }
    public void setMvp(boolean mvp) { this.mvp = mvp; }
    public Integer getKills() { return kills; }
    public void setKills(Integer kills) { this.kills = kills; }
    public Integer getDeaths() { return deaths; }
    public void setDeaths(Integer deaths) { this.deaths = deaths; }
    public Integer getAssists() { return assists; }
    public void setAssists(Integer assists) { this.assists = assists; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}