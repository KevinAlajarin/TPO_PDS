package com.scrim_pds.model;

import com.scrim_pds.model.enums.CanalNotificacion; 

import java.util.HashSet; 
import java.util.Set;

public class PreferenciasUsuario {

    // Valores posibles: "EMAIL", "PUSH", "DISCORD"
    private Set<String> canalesNotificacion = new HashSet<>(); 

    // Switches para activar/desactivar tipos de alertas
    private boolean alertasScrim = true; // Activado por defecto
    private boolean alertasPostulacion = true; // Activado por defecto
    private boolean recordatoriosActivos = true; // Activado por defecto

    // Preferencias de busqueda por defecto
    private String busquedaJuegoPorDefecto;
    private String busquedaRegionPorDefecto;
    private String busquedaRangoMinPorDefecto;
    private String busquedaRangoMaxPorDefecto;

    // Constructor
    public PreferenciasUsuario() {
        // Establecer valores por defecto si es necesario,
        canalesNotificacion.add(CanalNotificacion.EMAIL.name()); // Email por defecto
    }

    // Getters y Setters
    public Set<String> getCanalesNotificacion() {
        return canalesNotificacion;
    }

    public void setCanalesNotificacion(Set<String> canalesNotificacion) {
        this.canalesNotificacion = canalesNotificacion != null ? canalesNotificacion : new HashSet<>(); 
    }

    public boolean isAlertasScrim() {
        return alertasScrim;
    }

    public void setAlertasScrim(boolean alertasScrim) {
        this.alertasScrim = alertasScrim;
    }

    public boolean isAlertasPostulacion() {
        return alertasPostulacion;
    }

    public void setAlertasPostulacion(boolean alertasPostulacion) {
        this.alertasPostulacion = alertasPostulacion;
    }

    public boolean isRecordatoriosActivos() {
        return recordatoriosActivos;
    }

    public void setRecordatoriosActivos(boolean recordatoriosActivos) {
        this.recordatoriosActivos = recordatoriosActivos;
    }

    public String getBusquedaJuegoPorDefecto() {
        return busquedaJuegoPorDefecto;
    }

    public void setBusquedaJuegoPorDefecto(String busquedaJuegoPorDefecto) {
        this.busquedaJuegoPorDefecto = busquedaJuegoPorDefecto;
    }

    public String getBusquedaRegionPorDefecto() {
        return busquedaRegionPorDefecto;
    }

    public void setBusquedaRegionPorDefecto(String busquedaRegionPorDefecto) {
        this.busquedaRegionPorDefecto = busquedaRegionPorDefecto;
    }

    public String getBusquedaRangoMinPorDefecto() {
        return busquedaRangoMinPorDefecto;
    }

    public void setBusquedaRangoMinPorDefecto(String busquedaRangoMinPorDefecto) {
        this.busquedaRangoMinPorDefecto = busquedaRangoMinPorDefecto;
    }

    public String getBusquedaRangoMaxPorDefecto() {
        return busquedaRangoMaxPorDefecto;
    }

    public void setBusquedaRangoMaxPorDefecto(String busquedaRangoMaxPorDefecto) {
        this.busquedaRangoMaxPorDefecto = busquedaRangoMaxPorDefecto;
    }
}
