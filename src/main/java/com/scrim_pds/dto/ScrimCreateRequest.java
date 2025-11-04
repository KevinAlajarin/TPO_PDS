package com.scrim_pds.dto;

import com.scrim_pds.model.enums.Formato;
import com.scrim_pds.model.enums.MatchmakingStrategyType;
import com.scrim_pds.model.enums.Modalidad;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class ScrimCreateRequest {

    @NotBlank(message = "El juego no puede estar vacío")
    private String juego;

    @NotNull(message = "El formato no puede ser nulo")
    private Formato formato;

    @NotBlank(message = "La región no puede estar vacía")
    private String region;

    @NotBlank(message = "El rango mínimo no puede estar vacío")
    private String rangoMin;

    @NotBlank(message = "El rango máximo no puede estar vacío")
    private String rangoMax;

    @NotNull(message = "La latencia máxima no puede ser nula")
    @Min(value = 1, message = "La latencia debe ser positiva")
    private Integer latenciaMax;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La fecha y hora deben ser en el futuro")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaHora;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 10, message = "La duración debe ser de al menos 10 minutos")
    private Integer duracion;

    @NotNull(message = "La modalidad no puede ser nula")
    private Modalidad modalidad;

    @Schema(description = "Descripción opcional o reglas del scrim")
    private String descripcion;

    @NotNull(message = "El cupo es obligatorio")
    // --- ESTE ES EL ARREGLO PARA EL ERROR 400 ---
    // Cambiado de @Min(2) a @Min(1) para permitir 1v1
    @Min(value = 1, message = "El cupo debe ser al menos 1")
    private Integer cupo;

    @NotNull(message = "La estrategia de matchmaking es obligatoria")
    private MatchmakingStrategyType matchmakingStrategyType;

    // Getters y Setters (Jackson los necesita)

    public String getJuego() {
        return juego;
    }

    public void setJuego(String juego) {
        this.juego = juego;
    }

    public Formato getFormato() {
        return formato;
    }

    public void setFormato(Formato formato) {
        this.formato = formato;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRangoMin() {
        return rangoMin;
    }

    public void setRangoMin(String rangoMin) {
        this.rangoMin = rangoMin;
    }

    public String getRangoMax() {
        return rangoMax;
    }

    public void setRangoMax(String rangoMax) {
        this.rangoMax = rangoMax;
    }

    public Integer getLatenciaMax() {
        return latenciaMax;
    }

    public void setLatenciaMax(Integer latenciaMax) {
        this.latenciaMax = latenciaMax;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getDuracion() {
        return duracion;
    }

    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public MatchmakingStrategyType getMatchmakingStrategyType() {
        return matchmakingStrategyType;
    }

    public void setMatchmakingStrategyType(MatchmakingStrategyType matchmakingStrategyType) {
        this.matchmakingStrategyType = matchmakingStrategyType;
    }
}

