package com.scrim_pds.dto;

import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.PostulacionState;

import java.time.LocalDateTime;
import java.util.UUID;

// Este DTO combina la Postulación con el Username del postulante
public class PostulacionResponse {

    private UUID id;
    private UUID usuarioId;
    private UUID scrimId;
    private String rolDeseado;
    private PostulacionState estado;
    private int latenciaReportada;
    private LocalDateTime fechaPostulacion;
    private String username; // <-- ¡El campo que queremos!

    // Constructor que crea este DTO a partir de los dos objetos
    public PostulacionResponse(Postulacion postulacion, User user) {
        this.id = postulacion.getId();
        this.usuarioId = postulacion.getUsuarioId();
        this.scrimId = postulacion.getScrimId();
        this.rolDeseado = postulacion.getRolDeseado();
        this.estado = postulacion.getEstado();
        this.latenciaReportada = postulacion.getLatenciaReportada();
        this.fechaPostulacion = postulacion.getFechaPostulacion();
        this.username = (user != null) ? user.getUsername() : "Usuario Desconocido";
    }

    // Getters (necesarios para que Jackson lo convierta a JSON)
    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getScrimId() { return scrimId; }
    public String getRolDeseado() { return rolDeseado; }
    public PostulacionState getEstado() { return estado; }
    public int getLatenciaReportada() { return latenciaReportada; }
    public LocalDateTime getFechaPostulacion() { return fechaPostulacion; }
    public String getUsername() { return username; }
}