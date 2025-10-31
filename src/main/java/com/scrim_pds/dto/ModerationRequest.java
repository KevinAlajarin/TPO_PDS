package com.scrim_pds.dto;

import com.scrim_pds.model.enums.ModerationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para actualizar el estado de moderación de un feedback")
public class ModerationRequest {

    @Schema(description = "El nuevo estado de moderación (APROBADO o RECHAZADO)")
    @NotNull(message = "El estado no puede ser nulo")
    private ModerationState newState;

    // Getters y Setters
    public ModerationState getNewState() {
        return newState;
    }

    public void setNewState(ModerationState newState) {
        this.newState = newState;
    }
}
