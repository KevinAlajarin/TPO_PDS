package com.scrim_pds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Schema(description = "Datos para actualizar las preferencias de notificación y búsqueda del usuario")
public class PreferencesUpdateRequest {

    @Schema(description = "Canales por los que desea recibir notificaciones (EMAIL, PUSH, DISCORD)")
    @NotNull(message = "La lista de canales no puede ser nula (puede estar vacía)")
    private Set<String> canalesNotificacion;

    @Schema(description = "Activar/desactivar alertas de nuevos scrims que coincidan")
    @NotNull(message = "alertasScrim no puede ser nulo")
    private Boolean alertasScrim;

    @Schema(description = "Activar/desactivar alertas sobre postulaciones (ej. Lobby Armado)")
    @NotNull(message = "alertasPostulacion no puede ser nulo")
    private Boolean alertasPostulacion;

    @Schema(description = "Activar/desactivar recordatorios de scrims")
    @NotNull(message = "recordatoriosActivos no puede ser nulo")
    private Boolean recordatoriosActivos;

    @Schema(description = "Juego por defecto para búsquedas (puede ser nulo o vacío)")
    private String busquedaJuegoPorDefecto;

    @Schema(description = "Región por defecto para búsquedas (puede ser nulo o vacío)")
    private String busquedaRegionPorDefecto;

    @Schema(description = "Rango mínimo por defecto para búsquedas (puede ser nulo o vacío)")
    private String busquedaRangoMinPorDefecto;

    @Schema(description = "Rango máximo por defecto para búsquedas (puede ser nulo o vacío)")
    private String busquedaRangoMaxPorDefecto;

    // Getters y Setters
    public Set<String> getCanalesNotificacion() {
        return canalesNotificacion;
    }

    public void setCanalesNotificacion(Set<String> canalesNotificacion) {
        this.canalesNotificacion = canalesNotificacion;
    }

    public Boolean getAlertasScrim() {
        return alertasScrim;
    }

    public void setAlertasScrim(Boolean alertasScrim) {
        this.alertasScrim = alertasScrim;
    }

    public Boolean getAlertasPostulacion() {
        return alertasPostulacion;
    }

    public void setAlertasPostulacion(Boolean alertasPostulacion) {
        this.alertasPostulacion = alertasPostulacion;
    }

    public Boolean getRecordatoriosActivos() {
        return recordatoriosActivos;
    }

    public void setRecordatoriosActivos(Boolean recordatoriosActivos) {
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
