package com.scrim_pds.dto;

import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.PostulacionState;

import java.time.LocalDateTime;
import java.util.UUID;

public class PostulacionResponse {

    private UUID id;
    private UUID usuarioId;
    private UUID scrimId;
    private String rolDeseado;
    private PostulacionState estado;
    private int latenciaReportada;
    private LocalDateTime fechaPostulacion;
    private String username;
    private boolean hasConfirmed; // <-- 1. AÑADIR ESTE CAMPO

    public PostulacionResponse(Postulacion postulacion, User user) {
        this.id = postulacion.getId();
        this.usuarioId = postulacion.getUsuarioId();
        this.scrimId = postulacion.getScrimId();
        this.rolDeseado = postulacion.getRolDeseado();
        this.estado = postulacion.getEstado();
        this.latenciaReportada = postulacion.getLatenciaReportada();
        this.fechaPostulacion = postulacion.getFechaPostulacion();
        this.username = (user != null) ? user.getUsername() : "Usuario Desconocido";
        this.hasConfirmed = postulacion.getHasConfirmed(); // <-- 2. ASIGNAR EL VALOR
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getScrimId() { return scrimId; }
    public String getRolDeseado() { return rolDeseado; }
    public PostulacionState getEstado() { return estado; }
    public int getLatenciaReportada() { return latenciaReportada; }
    public LocalDateTime getFechaPostulacion() { return fechaPostulacion; }
    public String getUsername() { return username; }
    public boolean isHasConfirmed() { return hasConfirmed; } // <-- 3. AÑADIR EL GETTER
}