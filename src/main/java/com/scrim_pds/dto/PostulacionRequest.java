package com.scrim_pds.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public class PostulacionRequest {

    @NotEmpty
    private String rolDeseado;
    
    @Min(0)
    private int latenciaReportada;

    // Getters y Setters
    public String getRolDeseado() { return rolDeseado; }
    public void setRolDeseado(String rolDeseado) { this.rolDeseado = rolDeseado; }
    public int getLatenciaReportada() { return latenciaReportada; }
    public void setLatenciaReportada(int latenciaReportada) { this.latenciaReportada = latenciaReportada; }
}