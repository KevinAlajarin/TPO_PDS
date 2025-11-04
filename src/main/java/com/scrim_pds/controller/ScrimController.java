package com.scrim_pds.controller;

import com.scrim_pds.adapter.ICalAdapter; // <-- AÑADIDO IMPORT
import com.scrim_pds.config.AuthUser;
import com.scrim_pds.dto.EstadisticaRequest;
import com.scrim_pds.dto.PostulacionRequest;
import com.scrim_pds.dto.PostulacionResponse;
import com.scrim_pds.dto.ScrimCreateRequest;
import com.scrim_pds.exception.UnauthorizedException;
import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.service.ScrimService;
import com.scrim_pds.config.AuthUser;
import com.scrim_pds.model.User;
// --- IMPORT FALTANTE ---
import com.scrim_pds.model.enums.ScrimStateEnum;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders; // <-- AÑADIDO IMPORT
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import com.scrim_pds.model.enums.Formato;

// --- Swagger Imports ---
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter; // Para parametros
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // Para indicar token requerido
import io.swagger.v3.oas.annotations.tags.Tag;
// --- Fin Swagger Imports ---

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;
import java.util.Optional;
import com.scrim_pds.dto.MyScrimResponse; // <-- AÑADIR IMPORT
// --- AÑADIR IMPORT ---
import com.scrim_pds.dto.EstadisticaResponse;

@RestController
@RequestMapping("/api/scrims")
@Tag(name = "Scrims", description = "Endpoints para gestionar Scrims")
public class ScrimController {

    private final ScrimService scrimService;
    private final ICalAdapter iCalAdapter; // <-- AÑADIDO CAMPO

    // --- CONSTRUCTOR MODIFICADO ---
    public ScrimController(ScrimService scrimService, ICalAdapter iCalAdapter) {
        this.scrimService = scrimService;
        this.iCalAdapter = iCalAdapter; // <-- AÑADIDO AL CONSTRUCTOR
    }

    @Operation(summary = "Listar scrims disponibles con filtros opcionales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de scrims encontrados",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = Scrim.class)))
    })
    @GetMapping
    public ResponseEntity<List<Scrim>> getScrims(
            @Parameter(description = "Filtrar por nombre exacto del juego (ej. Valorant)", required = false)
            @RequestParam Optional<String> juego,

            @Parameter(description = "Filtrar por región exacta (ej. LATAM)", required = false)
            @RequestParam Optional<String> region,

            @Parameter(description = "Filtrar por rango mínimo exacto (ej. Oro)", required = false)
            @RequestParam Optional<String> rangoMin,

            @Parameter(description = "Filtrar por rango máximo exacto (ej. Platino)", required = false)
            @RequestParam Optional<String> rangoMax,

            @Parameter(description = "Filtrar por latencia máxima permitida (ej. 100)", required = false)
            @RequestParam Optional<Integer> latenciaMax,

            @Parameter(description = "Filtrar por formato exacto (FORMATO_1V1, FORMATO_5V5, etc)", required = false)
            @RequestParam Optional<Formato> formato,

            @Parameter(description = "Filtrar por fecha de inicio (formato YYYY-MM-DD)", required = false, example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fecha

    ) throws IOException {

        List<Scrim> scrims = scrimService.findScrims(juego, region, rangoMin, rangoMax, latenciaMax, formato, fecha);
        return ResponseEntity.ok(scrims);
    }

    // --- NUEVO ENDPOINT AÑADIDO ---

    @Operation(summary = "Descargar archivo .ics (iCalendar) para un scrim")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo .ics listo para descargar",
                    // Especificar que devuelve 'text/calendar'
                    content = @Content(mediaType = "text/calendar",
                            schema = @Schema(type = "string", example = "BEGIN:VCALENDAR..."))),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error al generar el archivo iCal")
    })
    @GetMapping("/{id}/calendar")
    public ResponseEntity<String> getScrimCalendar(
            @Parameter(description = "ID del scrim para generar el calendario")
            @PathVariable("id") UUID scrimId
    ) throws IOException {

        // 1. Buscar el Scrim (usando el nuevo método del servicio)
        Scrim scrim = scrimService.findScrimById(scrimId);

        // 2. Usar el Adapter para generar el string iCal
        String icalString = iCalAdapter.generarEventoCalendario(scrim);

        if (icalString == null) {
            // Si el adapter falló, devolver 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar el archivo iCal.");
        }

        // 3. Preparar Headers para la descarga
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8");
        // Sugerir al navegador que lo descargue como un archivo
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scrim-" + scrim.getJuego() + "-" + scrimId + ".ics\"");

        return new ResponseEntity<>(icalString, headers, HttpStatus.OK);
    }


    @Operation(summary = "Crear un nuevo scrim", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Scrim creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Scrim.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante")
    })
    @PostMapping
    public ResponseEntity<Scrim> createScrim(
            @Parameter(description = "Datos para crear el scrim") @Valid @RequestBody ScrimCreateRequest scrimRequest,
            @Parameter(hidden = true) // Oculta el parámetro @AuthUser de la UI de Swagger
            @AuthUser User organizador
    ) throws IOException {
        Scrim newScrim = scrimService.createScrim(scrimRequest, organizador);
        return new ResponseEntity<>(newScrim, HttpStatus.CREATED);
    }

    @Operation(summary = "Postularse a un scrim existente", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Postulación creada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Postulacion.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. ya postulado, scrim no en estado BUSCANDO)")
    })
    @PostMapping("/{id}/postulaciones")
    public ResponseEntity<Postulacion> postularseAScrim(
            @Parameter(description = "ID del scrim al que postularse") @PathVariable("id") UUID scrimId,
            @Parameter(description = "Rol deseado y latencia") @Valid @RequestBody PostulacionRequest postulacionRequest,
            @Parameter(hidden = true) @AuthUser User jugador
    ) throws IOException {
        Postulacion newPostulacion = scrimService.postularse(scrimId, postulacionRequest, jugador);
        return new ResponseEntity<>(newPostulacion, HttpStatus.CREATED);
    }

    @Operation(summary = "Confirmar participación en un scrim", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Confirmación registrada"),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. scrim no en estado LOBBY_ARMADO)")
    })
    @PostMapping("/{id}/confirmaciones")
    public ResponseEntity<Void> confirmarParticipacion(
            @Parameter(description = "ID del scrim a confirmar") @PathVariable("id") UUID scrimId,
            @Parameter(hidden = true) @AuthUser User jugador
    ) throws IOException {
        scrimService.confirmar(scrimId, jugador);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Iniciar manualmente un scrim (por el organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scrim iniciado (estado EN_JUEGO)"),
            @ApiResponse(responseCode = "401", description = "Token inválido, faltante o no es del organizador"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. scrim no en estado CONFIRMADO)")
    })
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Void> iniciarScrim(
            @Parameter(description = "ID del scrim a iniciar") @PathVariable("id") UUID scrimId,
            @Parameter(hidden = true) @AuthUser User organizador
    ) throws IOException {
        scrimService.iniciarScrim(scrimId, organizador);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Cancelar un scrim (por el organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scrim cancelado"),
            @ApiResponse(responseCode = "401", description = "Token inválido, faltante o no es del organizador"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. scrim ya iniciado o finalizado)")
    })
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarScrim(
            @Parameter(description = "ID del scrim a cancelar") @PathVariable("id") UUID scrimId,
            @Parameter(hidden = true) @AuthUser User organizador
    ) throws IOException {
        scrimService.cancelarScrim(scrimId, organizador);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Finalizar un scrim (por el organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scrim finalizado"),
            @ApiResponse(responseCode = "401", description = "Token inválido, faltante o no es del organizador"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. scrim no en estado EN_JUEGO o CONFIRMADO)")
    })
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Void> finalizarScrim(
            @Parameter(description = "ID del scrim a finalizar") @PathVariable("id") UUID scrimId,
            @Parameter(hidden = true) @AuthUser User organizador
    ) throws IOException {
        scrimService.finalizarScrim(scrimId, organizador);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Subir estadísticas de un scrim finalizado (por el organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Estadísticas guardadas"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (lista vacía o DTOs inválidos)"),
            @ApiResponse(responseCode = "401", description = "Token inválido, faltante o no es del organizador"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto (ej. scrim no en estado FINALIZADO o stats ya existen)")
    })
    @PostMapping("/{id}/estadisticas")
    public ResponseEntity<Void> subirEstadisticas(
            @Parameter(description = "ID del scrim finalizado") @PathVariable("id") UUID scrimId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Lista de estadísticas por jugador")
            @Valid @RequestBody List<EstadisticaRequest> estadisticas,
            @Parameter(hidden = true) @AuthUser User organizador
    ) throws IOException {
        if (estadisticas == null || estadisticas.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        scrimService.guardarEstadisticas(scrimId, estadisticas, organizador);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- INICIO: NUEVO ENDPOINT AÑADIDO ---
    @Operation(summary = "Obtener las estadísticas de un scrim finalizado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de estadísticas",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = EstadisticaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado")
    })
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<List<EstadisticaResponse>> getEstadisticas(
            @Parameter(description = "ID del scrim") @PathVariable("id") UUID scrimId
    ) throws IOException {
        // Nota: Este endpoint es público, cualquiera puede ver los resultados
        // de un scrim finalizado.
        List<EstadisticaResponse> estadisticas = scrimService.getEstadisticasForScrim(scrimId);
        return ResponseEntity.ok(estadisticas);
    }
    // --- FIN: NUEVO ENDPOINT AÑADIDO ---


    // --- AÑADIR ESTE NUEVO ENDPOINT ---
    @Operation(summary = "Listar los scrims del usuario (organizados y postulados)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de scrims del usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = Scrim.class))),
            @ApiResponse(responseCode = "401", description = "Token inválido o faltante")
    })
    @GetMapping("/my-scrims") // <-- Esta es la ruta que faltaba
    public ResponseEntity<List<MyScrimResponse>> getMyScrims( // <-- CAMBIO DE TIPO DE RETORNO
                                                              @Parameter(hidden = true)
                                                              @AuthUser User authenticatedUser
    ) throws IOException {

        List<MyScrimResponse> myScrims = scrimService.findMyScrims(authenticatedUser); // El servicio ya devuelve el DTO
        return ResponseEntity.ok(myScrims);
    }
    // --- FIN DEL NUEVO ENDPOINT ---

    // --- AÑADIR ESTE NUEVO ENDPOINT ---

    @Operation(summary = "Obtener los detalles de un scrim específico por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalles del Scrim",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Scrim.class))),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado")
    })
    @GetMapping("/{id}") // <-- Esta es la ruta que faltaba
    public ResponseEntity<Scrim> getScrimById(
            @Parameter(description = "ID del scrim a buscar")
            @PathVariable("id") UUID scrimId
    ) throws IOException {

        // Usamos el método que ya existía en tu ScrimService
        Scrim scrim = scrimService.findScrimById(scrimId);
        return ResponseEntity.ok(scrim);
    }
    // --- FIN DEL NUEVO ENDPOINT ---

    // --- AÑADIR ESTE NUEVO ENDPOINT (ANTES O DESPUÉS DEL POST A LA MISMA RUTA) ---

    @Operation(summary = "Listar todas las postulaciones para un scrim (Solo Organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de postulaciones",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = Postulacion.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Scrim no encontrado")
    })
    @GetMapping("/{id}/postulaciones") // <-- Esta es la ruta GET que faltaba


// --- 5. CAMBIO DE TIPO DE RETORNO ---
    public ResponseEntity<List<PostulacionResponse>> getPostulaciones(
            @Parameter(description = "ID del scrim") @PathVariable("id") UUID scrimId,
            @Parameter(hidden = true) @AuthUser User authenticatedUser
    ) throws IOException {

        // 1. Cargamos el scrim y TODAS las postulaciones (con usernames)
        Scrim scrim = scrimService.findScrimById(scrimId);
        List<PostulacionResponse> postulaciones = scrimService.getPostulacionesForScrim(scrimId);

        // 2. Chequeamos si es el organizador
        boolean isOrganizador = scrim.getOrganizadorId().equals(authenticatedUser.getId());

        // --- INICIO DE LA MODIFICACIÓN (DE LA VEZ PASADA) ---

        // 3. Si es el Owner, O si el Scrim está FINALIZADO, todos ven la lista completa
        //    (para poder dejar feedback)
        if (isOrganizador || scrim.getEstado() == ScrimStateEnum.FINALIZADO) {
            return ResponseEntity.ok(postulaciones);
        }

        // 4. Si no, es un jugador normal en un Scrim activo/pendiente.
        //    Solo debe ver su propia postulación.
        Optional<PostulacionResponse> myPostulacion = postulaciones.stream()
                .filter(p -> p.getUsuarioId().equals(authenticatedUser.getId()))
                .findFirst();

        if (myPostulacion.isPresent()) {
            return ResponseEntity.ok(Collections.singletonList(myPostulacion.get()));
        } else {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // --- FIN DE LA MODIFICACIÓN ---
    }

    // --- 6. AÑADIR NUEVOS ENDPOINTS ---

    @Operation(summary = "Aceptar una postulación (Solo Organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Postulación Aceptada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Scrim o Postulación no encontrada")
    })
    @PostMapping("/{id}/postulaciones/{postulacionId}/aceptar")
    public ResponseEntity<Void> aceptarPostulacion(
            @Parameter(description = "ID del scrim") @PathVariable("id") UUID scrimId,
            @Parameter(description = "ID de la postulación") @PathVariable("postulacionId") UUID postulacionId,
            @Parameter(hidden = true) @AuthUser User organizador
    ) throws IOException {

        scrimService.aceptarPostulacion(scrimId, postulacionId, organizador);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Rechazar una postulación (Solo Organizador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Postulación Rechazada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Scrim o Postulación no encontrada")
    })
    @PostMapping("/{id}/postulaciones/{postulacionId}/rechazar")
    public ResponseEntity<Void> rechazarPostulacion(
            @Parameter(description = "ID del scrim") @PathVariable("id") UUID scrimId,
            @Parameter(description = "ID de la postulación") @PathVariable("postulacionId") UUID postulacionId,
            @Parameter(hidden = true) @AuthUser User organizador
    ) throws IOException {

        scrimService.rechazarPostulacion(scrimId, postulacionId, organizador);
        return ResponseEntity.ok().build();
    }
    // --- FIN DE NUEVOS ENDPOINTS ---


}

