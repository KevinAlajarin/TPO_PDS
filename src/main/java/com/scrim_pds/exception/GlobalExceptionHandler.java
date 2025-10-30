package com.scrim_pds.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    //Maneja excepciones de validación de DTOs (@Valid).
    // Devuelve HTTP 400 Bad Request.

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Error de validación"
                ));
        return new ResponseEntity<>(Map.of("error", "Datos de entrada inválidos", "detalles", errors), HttpStatus.BAD_REQUEST);
    }

    //Maneja conflicto de usuario existente.
    //Devuelve HTTP 409 Conflict.

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT);
    }

    //Maneja credenciales inválidas en Login.
    // Devuelve HTTP 401 Unauthorized.

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // Maneja token inválido o faltante.
    // Devuelve HTTP 401 Unauthorized.

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    //Maneja scrim no encontrado.
    // Devuelve HTTP 404 Not Found.

    @ExceptionHandler(ScrimNotFoundException.class)
    public ResponseEntity<Object> handleScrimNotFound(ScrimNotFoundException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    //Maneja lógica de estado inválida (ej. postularse a scrim lleno).
    //Devuelve HTTP 409 Conflict.

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

    // Manejador genérico para CUALQUIER OTRA excepción no controlada.
    //Devuelve HTTP 500 Internal Server Error.

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        ex.printStackTrace(); 
        return new ResponseEntity<>(Map.of("error", "Ocurrió un error interno en el servidor."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
