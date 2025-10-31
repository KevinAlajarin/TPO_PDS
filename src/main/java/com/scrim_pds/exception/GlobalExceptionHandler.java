package com.scrim_pds.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

// --- AÑADIR IMPORT PARA LA NUEVA EXCEPCIÓN ---
import com.scrim_pds.exception.FeedbackNotAllowedException;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones de validación de DTOs (@Valid).
     * Devuelve HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Error de validación"
                ));
        return new ResponseEntity<>(Map.of("error", "Datos de entrada inválidos", "detalles", errors), HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja conflicto de usuario existente.
     * Devuelve HTTP 409 Conflict.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    /**
     * Maneja credenciales inválidas en Login.
     * Devuelve HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja token inválido o faltante.
     * Devuelve HTTP 401 Unauthorized.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja scrim no encontrado.
     * Devuelve HTTP 404 Not Found.
     */
    @ExceptionHandler(ScrimNotFoundException.class)
    public ResponseEntity<Object> handleScrimNotFound(ScrimNotFoundException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja lógica de estado inválida (ej. postularse a scrim lleno).
     * Devuelve HTTP 409 Conflict.
     */
    @ExceptionHandler(InvalidScrimStateException.class)
    public ResponseEntity<Object> handleInvalidScrimState(InvalidScrimStateException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleInvalidToken(InvalidTokenException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.BAD_REQUEST); // 400 Bad Request
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Object> handleTokenExpired(TokenExpiredException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.BAD_REQUEST); // 400 Bad Request
    }

    // --- NUEVO MANEJADOR AÑADIDO ---
    /**
     * Maneja intentos de feedback no permitidos (ej. no es participante, scrim no finalizado).
     * Devuelve HTTP 403 Forbidden.
     */
    @ExceptionHandler(FeedbackNotAllowedException.class)
    public ResponseEntity<Object> handleFeedbackNotAllowed(FeedbackNotAllowedException ex) {
        // 403 Forbidden es el código apropiado para "Sé quién eres, pero no tienes permiso para esta acción"
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.FORBIDDEN);
    }
    // --- FIN NUEVO MANEJADOR ---


    /**
     * Manejador genérico para CUALQUIER OTRA excepción no controlada.
     * Devuelve HTTP 500 Internal Server Error.
     * (DEBE IR AL FINAL)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        // Imprimir el stack trace en el log del servidor para depuración
        ex.printStackTrace(); 
        return new ResponseEntity<>(Map.of("error", "Ocurrió un error interno en el servidor."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

