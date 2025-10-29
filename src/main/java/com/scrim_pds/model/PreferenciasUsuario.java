package com.scrim_pds.model;

import com.scrim_pds.model.enums.CanalNotificacion; // Importar enum si lo usamos directamente

import java.util.HashSet; // Usar HashSet para inicializar
import java.util.Set;

public class PreferenciasUsuario {

    // Usaremos Set<String> para simplificar la serialización JSON.
    // Valores posibles: "EMAIL", "PUSH", "DISCORD"
    private Set<String> canalesNotificacion = new HashSet<>(); // Inicializar

    // Switches para activar/desactivar tipos de alertas
    private boolean alertasScrim = true; // Activado por defecto
    private boolean alertasPostulacion = true; // Activado por defecto
    private boolean recordatoriosActivos = true; // Activado por defecto

    // Preferencias de búsqueda por defecto
    private String busquedaJuegoPorDefecto;
    private String busquedaRegionPorDefecto;
    private String busquedaRangoMinPorDefecto;
    private String busquedaRangoMaxPorDefecto;

    // Constructor vacío para Jackson
    public PreferenciasUsuario() {
        // Establecer valores por defecto si es necesario,
        // aunque es mejor hacerlo al crear el usuario.
        canalesNotificacion.add(CanalNotificacion.EMAIL.name()); // Email por defecto
    }

    // Getters y Setters
    public Set<String> getCanalesNotificacion() {
        return canalesNotificacion;
    }

    public void setCanalesNotificacion(Set<String> canalesNotificacion) {
        this.canalesNotificacion = canalesNotificacion != null ? canalesNotificacion : new HashSet<>(); // Evitar null
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
