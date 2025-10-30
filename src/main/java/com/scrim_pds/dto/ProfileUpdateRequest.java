package com.scrim_pds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Schema(description = "Datos para actualizar el perfil público de un usuario")
public class ProfileUpdateRequest {

    @Schema(description = "Nuevo nombre de usuario (debe ser único)", example = "MatiasElOrganizador")
    @NotEmpty(message = "El nombre de usuario no puede estar vacío.")
    @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres.")
    private String username;

    @Schema(description = "Región o servidor principal del usuario", example = "SA (South America)")
    @NotEmpty(message = "La región no puede estar vacía.")
    private String region;

    @Schema(description = "Mapa de rangos del usuario por juego", example = "{\"Valorant\": \"Diamante\", \"LoL\": \"Oro\"}")
    private Map<String, String> rangoPorJuego;

    @Schema(description = "Lista de roles preferidos por el usuario", example = "[\"Duelista\", \"Iniciador\"]")
    private List<String> rolesPreferidos;

    // Getters y Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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
}
