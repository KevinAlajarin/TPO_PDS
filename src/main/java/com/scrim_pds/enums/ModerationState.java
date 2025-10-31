package com.scrim_pds.model.enums;

/**
 * Define los estados de moderación para el feedback.
 */
public enum ModerationState {
    PENDIENTE, // Recién creado, esperando revisión
    APROBADO,  // Visible públicamente (o para el usuario)
    RECHAZADO  // Oculto
}
