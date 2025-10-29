package com.scrim_pds.dto;

import com.scrim_pds.model.enums.Formato;
import com.scrim_pds.model.enums.MatchmakingStrategyType;
import com.scrim_pds.model.enums.Modalidad;
import io.swagger.v3.oas.annotations.media.Schema; // <-- Importar Schema
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Datos requeridos para crear un nuevo Scrim") // Descripción del DTO
public class ScrimCreateRequest {

    @Schema(description = "Nombre del juego", example = "Valorant") // Descripción y ejemplo para Swagger
    @NotEmpty(message = "El juego no puede estar vacío.")
    private String juego;

    @Schema(description = "Formato del scrim (ej. 1v1, 5v5)")
    @NotNull(message = "El formato no puede ser nulo.")
    private Formato formato;

    @Schema(description = "Región del servidor", example = "LATAM")
    @NotEmpty(message = "La región no puede estar vacía.")
    private String region;

    @Schema(description = "Rango mínimo requerido", example = "Oro")
    @NotEmpty(message = "El rango mínimo no puede estar vacío.")
    private String rangoMin;

    @Schema(description = "Rango máximo permitido", example = "Platino")
    @NotEmpty(message = "El rango máximo no puede estar vacío.")
    private String rangoMax;

    @Schema(description = "Latencia máxima en ms", example = "100")
    @NotNull(message = "La latencia máxima no puede ser nula.")
    @Min(value = 0, message = "La latencia máxima debe ser 0 o mayor.")
    private Integer latenciaMax; // Usar Integer

    @Schema(description = "Fecha y hora de inicio (formato ISO)", example = "2026-11-20T20:00:00")
    @NotNull(message = "La fecha/hora no puede ser nula.")
    @Future(message = "La fecha y hora deben ser en el futuro.")
    private LocalDateTime fechaHora;

    @Schema(description = "Duración estimada en minutos", example = "60")
    @NotNull(message = "La duración no puede ser nula.")
    @Min(value = 1, message = "La duración debe ser al menos 1 minuto.")
    private Integer duracion; // Usar Integer

    @Schema(description = "Modalidad del scrim")
    @NotNull(message = "La modalidad no puede ser nula.")
    private Modalidad modalidad;

    @Schema(description = "Descripción adicional (opcional)", example = "Scrim de práctica para torneo")
    private String descripcion; // Opcional

    @Schema(description = "Número total de jugadores requeridos", example = "10")
    @NotNull(message = "El cupo no puede ser nulo.")
    @Min(value = 2, message = "El cupo debe ser al menos 2.") // 1v1 = 2
    private Integer cupo; // Usar Integer

    @Schema(description = "Estrategia de matchmaking a usar (si aplica)", example = "BY_MMR")
    @NotNull(message = "El tipo de estrategia de matchmaking no puede ser nulo.")
    private MatchmakingStrategyType matchmakingStrategyType; // <-- Añadido

    // Getters y Setters
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
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getCupo() { return cupo; }
    public void setCupo(Integer cupo) { this.cupo = cupo; }
    public MatchmakingStrategyType getMatchmakingStrategyType() { return matchmakingStrategyType; } // <-- Nuevo
    public void setMatchmakingStrategyType(MatchmakingStrategyType matchmakingStrategyType) { this.matchmakingStrategyType = matchmakingStrategyType; } // <-- Nuevo
}

