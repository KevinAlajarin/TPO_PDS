package com.scrim_pds.model;

import com.scrim_pds.model.enums.Formato;
import com.scrim_pds.model.enums.MatchmakingStrategyType; 
import com.scrim_pds.model.enums.Modalidad;
import com.scrim_pds.model.enums.ScrimStateEnum;
import io.swagger.v3.oas.annotations.media.Schema; 

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects; 

public class Scrim {
    private UUID id;
    private String juego;
    private Formato formato;
    private String region;
    private String rangoMin;
    private String rangoMax;
    private Integer latenciaMax; 
    private LocalDateTime fechaHora;
    private Integer duracion; 
    private Modalidad modalidad;
    private UUID organizadorId;
    private ScrimStateEnum estado;
    private String descripcion;
    private Integer cupo; 
    private MatchmakingStrategyType matchmakingStrategyType; 
    
    @Schema(description = "Flag para saber si el recordatorio de 2 horas ya fue enviado", hidden = true) 
    private boolean recordatorioEnviado = false; // Default false

    public Scrim() { }

    // Getters and Setters 
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getJuego() { return juego; }
    public void setJuego(String juego) { this.juego = juego; }
    public Formato getFormato() { return formato; }
    public void setFormato(Formato formato) { this.formato = formato; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getRangoMin() { return rangoMin; }
    public void setRangoMin(String rangoMin) { this.rangoMin = rangoMin; }
    public String getRangoMax() { return rangoMax; }
    public void setRangoMax(String rangoMax) { this.rangoMax = rangoMax; }
    public Integer getLatenciaMax() { return latenciaMax; }
    public void setLatenciaMax(Integer latenciaMax) { this.latenciaMax = latenciaMax; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getDuracion() { return duracion; }
    public void setDuracion(Integer duracion) { this.duracion = duracion; }
    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
    public UUID getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(UUID organizadorId) { this.organizadorId = organizadorId; }
    public ScrimStateEnum getEstado() { return estado; }
    public void setEstado(ScrimStateEnum estado) { this.estado = estado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getCupo() { return cupo; }
    public void setCupo(Integer cupo) { this.cupo = cupo; }
    public MatchmakingStrategyType getMatchmakingStrategyType() { return matchmakingStrategyType; }
    public void setMatchmakingStrategyType(MatchmakingStrategyType matchmakingStrategyType) { this.matchmakingStrategyType = matchmakingStrategyType; }
    
    public boolean isRecordatorioEnviado() {
        return recordatorioEnviado;
    }
    
    public void setRecordatorioEnviado(boolean recordatorioEnviado) {
        this.recordatorioEnviado = recordatorioEnviado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scrim scrim = (Scrim) o;
        return id != null ? id.equals(scrim.id) : scrim.id == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); 
    }

    @Override
    public String toString() {
        return "Scrim{" +
               "id=" + id +
               ", juego='" + juego + '\'' +
               ", estado=" + estado +
               ", organizadorId=" + organizadorId +
               '}';
    }
}

