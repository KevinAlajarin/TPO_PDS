package com.scrim_pds.controller;

import com.scrim_pds.config.AuthUser;
import com.scrim_pds.dto.FeedbackCreateRequest;
import com.scrim_pds.dto.FeedbackResponse; // <-- 1. IMPORTAR NUEVO DTO
import com.scrim_pds.dto.ModerationRequest;
import com.scrim_pds.exception.FeedbackNotAllowedException;
import com.scrim_pds.model.Feedback;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.UserRole;
import com.scrim_pds.service.FeedbackService;
// ... (otros imports)
import java.io.IOException;
import java.util.List;
import java.util.UUID;
// ... (imports de swagger)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@Tag(name = "Feedback", description = "Endpoints para gestionar feedback y ratings post-scrim")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ... (POST /api/scrims/{id}/feedback no cambia) ...
    @Operation(summary = "Enviar feedback para un jugador después de un scrim", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Feedback enviado exitosamente (pendiente de moderación)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Feedback.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante"),
            @ApiResponse(responseCode = "403", description = "No permitido (ej. no participó, scrim no finalizado, auto-evaluación)"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (feedback ya enviado a este usuario para este scrim)")
    })
    @PostMapping("/api/scrims/{id}/feedback")
    public ResponseEntity<Feedback> createFeedback(
            @Parameter(description = "ID del Scrim finalizado") @PathVariable("id") UUID scrimId,
            @Parameter(hidden = true) @AuthUser User reviewer,
            @Valid @RequestBody FeedbackCreateRequest dto
    ) throws IOException {

        Feedback newFeedback = feedbackService.createFeedback(scrimId, reviewer, dto);
        return new ResponseEntity<>(newFeedback, HttpStatus.CREATED);
    }


    @Operation(summary = "Obtener todo el feedback APROBADO para un scrim")
    @ApiResponses(value = {
            // --- 2. MODIFICAR EL SCHEMA DE RESPUESTA ---
            @ApiResponse(responseCode = "200", description = "Lista de feedback aprobado (con usernames)",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = FeedbackResponse.class))),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado")
    })
    @GetMapping("/api/scrims/{id}/feedback")
    // --- 3. MODIFICAR EL TIPO DE RETORNO ---
    public ResponseEntity<List<FeedbackResponse>> getFeedbackForScrim(
            @Parameter(description = "ID del Scrim") @PathVariable("id") UUID scrimId
    ) throws IOException {

        List<FeedbackResponse> feedbackList = feedbackService.getApprovedFeedbackForScrim(scrimId);
        return ResponseEntity.ok(feedbackList);
    }

    // ... (Endpoints de Admin no cambian) ...
    @Operation(summary = "ADMIN: Obtener todo el feedback PENDIENTE de moderación", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de feedback pendiente"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos de administrador")
    })
    @GetMapping("/api/admin/feedback/pending")
    public ResponseEntity<List<Feedback>> getPendingFeedback(
            @Parameter(hidden = true) @AuthUser User admin
    ) throws IOException {
        // 1. Validación de Rol (Básica)
        if (admin.getRol() != UserRole.ADMIN) {
            throw new FeedbackNotAllowedException("No tienes permisos de administrador."); // Reusamos 403
        }

        List<Feedback> feedbackList = feedbackService.getPendingFeedback();
        return ResponseEntity.ok(feedbackList);
    }

    @Operation(summary = "ADMIN: Aprobar o Rechazar un feedback", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback moderado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. estado PENDIENTE)"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Feedback ID no encontrado")
    })
    @PostMapping("/api/admin/feedback/{feedbackId}/moderate")
    public ResponseEntity<Feedback> moderateFeedback(
            @Parameter(description = "ID del feedback a moderar") @PathVariable("feedbackId") UUID feedbackId,
            @Parameter(hidden = true) @AuthUser User admin,
            @Valid @RequestBody ModerationRequest dto
    ) throws IOException {

        // 1. Validación de Rol
        if (admin.getRol() != UserRole.ADMIN) {
            throw new FeedbackNotAllowedException("No tienes permisos de administrador.");
        }

        Feedback updatedFeedback = feedbackService.moderateFeedback(feedbackId, dto);
        return ResponseEntity.ok(updatedFeedback);
    }
}