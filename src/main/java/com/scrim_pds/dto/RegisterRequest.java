package com.scrim_pds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set; // Importar Set

public class RegisterRequest {

    @Schema(description = "Nombre de usuario único", example = "jugador123")
    @NotEmpty(message = "El nombre de usuario no puede estar vacío.")
    @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres.")
    private String username;

    @Schema(description = "Email único para login y notificaciones", example = "jugador@example.com")
    @NotEmpty(message = "El email no puede estar vacío.")
    @Email(message = "Debe ser un email válido.")
    private String email;

    @Schema(description = "Contraseña (mínimo 8 caracteres)", example = "passwordSegura123")
    @NotEmpty(message = "La contraseña no puede estar vacía.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    private String password;

    // --- NUEVOS CAMPOS OPCIONALES PARA PREFERENCIAS ---
    @Schema(description = "Juego principal preferido (opcional)", example = "Valorant", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String juegoPreferido;

    @Schema(description = "Región principal preferida (opcional)", example = "LATAM", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String regionPreferida;

    @Schema(description = "Rango mínimo para alertas (opcional)", example = "Oro", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String rangoMinPreferido;

     @Schema(description = "Rango máximo para alertas (opcional)", example = "Diamante", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String rangoMaxPreferido;

    @Schema(description = "Canales de notificación preferidos (opcional, valores: EMAIL, PUSH, DISCORD)", example = "[\"EMAIL\", \"PUSH\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> canalesPreferidos; // Set de Strings

    // Getters y Setters (para todos los campos, incluyendo los nuevos)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getJuegoPreferido() { return juegoPreferido; }
    public void setJuegoPreferido(String juegoPreferido) { this.juegoPreferido = juegoPreferido; }
    public String getRegionPreferida() { return regionPreferida; }
    public void setRegionPreferida(String regionPreferida) { this.regionPreferida = regionPreferida; }
    public String getRangoMinPreferido() { return rangoMinPreferido; }
    public void setRangoMinPreferido(String rangoMinPreferido) { this.rangoMinPreferido = rangoMinPreferido; }
    public String getRangoMaxPreferido() { return rangoMaxPreferido; }
    public void setRangoMaxPreferido(String rangoMaxPreferido) { this.rangoMaxPreferido = rangoMaxPreferido; }
    public Set<String> getCanalesPreferidos() { return canalesPreferidos; }
    public void setCanalesPreferidos(Set<String> canalesPreferidos) { this.canalesPreferidos = canalesPreferidos; }
}
