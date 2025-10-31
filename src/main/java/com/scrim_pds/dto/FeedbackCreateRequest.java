package com.scrim_pds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Datos para enviar feedback sobre otro jugador después de un scrim")
public class FeedbackCreateRequest {

    @Schema(description = "ID del usuario que está siendo evaluado (target)")
    @NotNull(message = "El ID del usuario objetivo no puede ser nulo")
    private UUID targetUserId;

    @Schema(description = "Calificación de 1 a 5 estrellas")
    @NotNull(message = "El rating no puede ser nulo")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer rating;

    @Schema(description = "Comentario sobre el jugador (opcional)", maxLength = 500)
    @Size(max = 500, message = "El comentario no puede exceder los 500 caracteres")
    private String comment;

    // Getters y Setters
    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
