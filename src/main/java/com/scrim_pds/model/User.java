package com.scrim_pds.model;

import com.scrim_pds.model.enums.UserRole;
import com.scrim_pds.model.enums.VerificationState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class User {
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private Map<String, String> rangoPorJuego; 
    private List<String> rolesPreferidos; 
    private String region;
    private PreferenciasUsuario preferencias;
    private UserRole rol; 
    private int strikes;
    private LocalDateTime cooldownHasta;
    private VerificationState estadoVerificacion; 

    // Constructor
    public User() {
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Map<String, String> getRangoPorJuego() {
        return rangoPorJuego;
    }

    public void setRangoPorJuego(Map<String, String> rangoPorJuego) {
        this.rangoPorJuego = rangoPorJuego;
    }

    public List<String> getRolesPreferidos() {
        return rolesPreferidos;
    }

    public void setRolesPreferidos(List<String> rolesPreferidos) {
        this.rolesPreferidos = rolesPreferidos;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public PreferenciasUsuario getPreferencias() {
        return preferencias;
    }

    public void setPreferencias(PreferenciasUsuario preferencias) {
        this.preferencias = preferencias;
    }

    public UserRole getRol() {
        return rol;
    }

    public void setRol(UserRole rol) {
        this.rol = rol;
    }

    public int getStrikes() {
        return strikes;
    }

    public void setStrikes(int strikes) {
        this.strikes = strikes;
    }

    public LocalDateTime getCooldownHasta() {
        return cooldownHasta;
    }

    public void setCooldownHasta(LocalDateTime cooldownHasta) {
        this.cooldownHasta = cooldownHasta;
    }

    public VerificationState getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(VerificationState estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }
}