package com.scrim_pds.dto;

import com.scrim_pds.model.Feedback;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.ModerationState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "DTO para el feedback, incluyendo nombres de usuario")
public class FeedbackResponse {

    private UUID id;
    private UUID scrimId;
    private int rating;
    private String comment;
    private ModerationState moderationState;
    private LocalDateTime createdAt;

    private UUID reviewerId;
    private String reviewerUsername; // Dato enriquecido

    private UUID targetUserId;
    private String targetUsername; // Dato enriquecido

    public FeedbackResponse(Feedback feedback, User reviewer, User target) {
        this.id = feedback.getId();
        this.scrimId = feedback.getScrimId();
        this.rating = feedback.getRating();
        this.comment = feedback.getComment();
        this.moderationState = feedback.getModerationState();
        this.createdAt = feedback.getCreatedAt();

        this.reviewerId = feedback.getReviewerId();
        this.reviewerUsername = (reviewer != null) ? reviewer.getUsername() : "Usuario Desconocido";

        this.targetUserId = feedback.getTargetUserId();
        this.targetUsername = (target != null) ? target.getUsername() : "Usuario Desconocido";
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getScrimId() { return scrimId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public ModerationState getModerationState() { return moderationState; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UUID getReviewerId() { return reviewerId; }
    public String getReviewerUsername() { return reviewerUsername; }
    public UUID getTargetUserId() { return targetUserId; }
    public String getTargetUsername() { return targetUsername; }
}