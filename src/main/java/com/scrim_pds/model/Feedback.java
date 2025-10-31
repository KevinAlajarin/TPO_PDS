package com.scrim_pds.model;

import com.scrim_pds.model.enums.ModerationState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Representa una pieza de feedback (rating/comentario) de un usuario a otro después de un scrim")
public class Feedback {

    private UUID id;
    private UUID scrimId; // Scrim donde ocurrió
    private UUID reviewerId; // Usuario que ESCRIBE el feedback
    private UUID targetUserId; // Usuario que RECIBE el feedback
    private int rating; // Ej. 1-5 estrellas
    private String comment;
    private ModerationState moderationState; // PENDIENTE, APROBADO, RECHAZADO
    private LocalDateTime createdAt;

    // Constructor vacío para Jackson
    public Feedback() {
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getScrimId() {
        return scrimId;
    }

    public void setScrimId(UUID scrimId) {
        this.scrimId = scrimId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ModerationState getModerationState() {
        return moderationState;
    }

    public void setModerationState(ModerationState moderationState) {
        this.moderationState = moderationState;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
