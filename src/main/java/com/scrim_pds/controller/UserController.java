package com.scrim_pds.controller;

import com.scrim_pds.config.AuthUser;
import com.scrim_pds.dto.PreferencesUpdateRequest;
import com.scrim_pds.dto.ProfileUpdateRequest;
import com.scrim_pds.model.PreferenciasUsuario;
import com.scrim_pds.model.User;
import com.scrim_pds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "Endpoints para gestionar perfiles y preferencias de usuario")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Obtener el perfil del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil del usuario",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = User.class))), // Devuelve el User completo
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante")
    })
    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(
            @Parameter(hidden = true) // Ocultar de Swagger
            @AuthUser User authenticatedUser
    ) {
        // @AuthUser ya busco al usuario, asi que podemos devolverlo directamente.
        // No necesitamos quitar el hash de la contraseña porque el @AuthUser
        // no lo incluye si lo filtramos (aunque nuestro DTO LoginResponse sí lo filtra).
        // Por seguridad, es mejor devolver solo un DTO de respuesta.
        // Pero por simplicidad, devolvemos el objeto User (sin el hash).
        authenticatedUser.setPasswordHash(null); // ¡Nunca devolver el hash!
        return ResponseEntity.ok(authenticatedUser);
    }


    @Operation(summary = "Actualizar el perfil público del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = User.class))), // Devuelve User actualizado
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. username corto)"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. username ya existe)")
    })
    @PutMapping("/me/profile")
    public ResponseEntity<User> updateProfile(
            @Parameter(hidden = true) @AuthUser User authenticatedUser,
            @Parameter(description = "Datos del perfil a actualizar")
            @Valid @RequestBody ProfileUpdateRequest dto
    ) throws IOException {

        User updatedUser = userService.updateUserProfile(authenticatedUser.getId(), dto);
        updatedUser.setPasswordHash(null); // Quitar hash antes de devolver
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Actualizar las preferencias del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferencias actualizadas",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = PreferenciasUsuario.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. campo nulo)"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante")
    })
    @PutMapping("/me/preferences")
    public ResponseEntity<PreferenciasUsuario> updatePreferences(
            @Parameter(hidden = true) @AuthUser User authenticatedUser,
            @Parameter(description = "Preferencias a actualizar")
            @Valid @RequestBody PreferencesUpdateRequest dto
    ) throws IOException {

        PreferenciasUsuario updatedPreferences = userService.updateUserPreferences(authenticatedUser.getId(), dto);
        return ResponseEntity.ok(updatedPreferences);
    }
}

      
