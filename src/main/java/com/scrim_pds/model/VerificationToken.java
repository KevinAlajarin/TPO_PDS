package com.scrim_pds.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class VerificationToken {
    private String token;
    private UUID userId;
    private LocalDateTime expiresAt;

    // Constructor vacío para Jackson
    public VerificationToken() {
    }

    public VerificationToken(String token, UUID userId, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
