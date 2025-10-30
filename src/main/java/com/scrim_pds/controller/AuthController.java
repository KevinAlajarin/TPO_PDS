package com.scrim_pds.controller;

import com.scrim_pds.dto.LoginRequest;
import com.scrim_pds.dto.LoginResponse;
import com.scrim_pds.dto.RegisterRequest;
import com.scrim_pds.model.User;
import com.scrim_pds.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; 

// --- Swagger Imports ---
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter; 
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
// --- Fin Swagger Imports ---

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
// Modificar Tag description
@Tag(name = "Authentication", description = "Endpoints para registro, login y verificación de email")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // --- ENDPOINT REGISTER ---
    @Operation(summary = "Registrar un nuevo usuario (incluyendo preferencias opcionales)") // Descripcion actualizada
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(example = "{\"mensaje\":\"Usuario registrado exitosamente. Por favor verifica tu email.\",\"userId\":\"uuid-goes-here\"}"))), // Mensaje actualizado
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ver detalles)",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(example = "{\"error\":\"Datos de entrada inválidos\",\"detalles\":{\"username\":\"...\",\"email\":\"...\"}}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto - El email o usuario ya existe",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(example = "{\"error\":\"El email test@example.com ya está en uso.\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(
            @Parameter(description = "Datos del usuario a registrar") // Descripción de parámetro
            @Valid @RequestBody RegisterRequest registerRequest) throws IOException {

        // --- Pasar el DTO completo ---
        User newUser = authService.register(registerRequest);

        return new ResponseEntity<>(
                // Mensaje actualizado para indicar verificación
                Map.of("mensaje", "Usuario registrado exitosamente. Por favor verifica tu email.", "userId", newUser.getId()),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "Autenticar un usuario y obtener un token de sesión")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso, devuelve token y datos del usuario",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o email no verificado (si está habilitado)",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(example = "{\"error\":\"Email o contraseña incorrectos.\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) throws IOException {

        LoginResponse loginResponse = authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Verificar el email de un usuario usando un token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verificado exitosamente",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(example = "{\"mensaje\":\"Email verificado correctamente.\"}"))),
            @ApiResponse(responseCode = "400", description = "Token inválido o expirado",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(example = "{\"error\":\"El token de verificación ha expirado.\"}")))
    })
    @GetMapping("/verify")
    public ResponseEntity<Object> verifyEmail(
            @Parameter(description = "Token de verificación recibido por email", required = true, example = "AbCdEfGhIjKlMnOpQrStUvWxYz123456AbCdEfGhIjKlM")
            @RequestParam("token") String token) {

        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("mensaje", "Email verificado correctamente."));
        } catch (IOException e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(Map.of("error", "Error interno al procesar la verificación."));
        }
        // InvalidTokenException y TokenExpiredException son manejadas por GlobalExceptionHandler
    }
}