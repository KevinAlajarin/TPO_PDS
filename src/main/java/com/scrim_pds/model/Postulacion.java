package com.scrim_pds.model;

import com.scrim_pds.model.enums.PostulacionState;

import java.time.LocalDateTime;
import java.util.UUID;

public class Postulacion {
    private UUID id;
    private UUID usuarioId;
    private UUID scrimId;
    private String rolDeseado; // Ej: "Duelista"
    private PostulacionState estado; // PENDIENTE, ACEPTADA, RECHAZADA
    private int latenciaReportada;
    private LocalDateTime fechaPostulacion;

    // Constructor sin argumentos para Jackson
    public Postulacion() {
    }
    
    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getScrimId() {
        return scrimId;
    }

    public void setScrimId(UUID scrimId) {
        this.scrimId = scrimId;
    }

    public String getRolDeseado() {
        return rolDeseado;
    }

    public void setRolDeseado(String rolDeseado) {
        this.rolDeseado = rolDeseado;
    }

    public PostulacionState getEstado() {
        return estado;
    }

    public void setEstado(PostulacionState estado) {
        this.estado = estado;
    }

    public int getLatenciaReportada() {
        return latenciaReportada;
    }

    public void setLatenciaReportada(int latenciaReportada) {
        this.latenciaReportada = latenciaReportada;
    }

    public LocalDateTime getFechaPostulacion() {
        return fechaPostulacion;
    }

    public void setFechaPostulacion(LocalDateTime fechaPostulacion) {
        this.fechaPostulacion = fechaPostulacion;
    }
}