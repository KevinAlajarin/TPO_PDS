package com.scrim_pds.service;

import com.scrim_pds.dto.LoginResponse;
import com.scrim_pds.dto.RegisterRequest; 
import com.scrim_pds.exception.InvalidCredentialsException;
import com.scrim_pds.exception.UserAlreadyExistsException;
import com.scrim_pds.exception.TokenExpiredException;
import com.scrim_pds.exception.InvalidTokenException;
import com.scrim_pds.model.*; 
import com.scrim_pds.model.enums.CanalNotificacion; 
import com.scrim_pds.model.enums.UserRole;
import com.scrim_pds.model.enums.VerificationState;
import com.scrim_pds.notification.NotificationService;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors; 

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final JsonPersistenceManager persistenceManager;
    private final NotificationService notificationService;
    private final String USERS_FILE = "users.json";
    private final String SESSIONS_FILE = "sessions.json";
    private final String VERIFICATIONS_FILE = "verifications.json";

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Value("${app.base-url}")
    private String appBaseUrl;

    public AuthService(JsonPersistenceManager persistenceManager,
                       NotificationService notificationService) {
        this.persistenceManager = persistenceManager;
        this.notificationService = notificationService;
    }

    /**
     * Registra un nuevo usuario, guarda preferencias, genera token y envia email.
     * @param dto El DTO con los datos de registro, incluyendo preferencias opcionales.
     * @return El usuario creado.
     * @throws IOException Si ocurre un error al leer/escribir archivos.
     * @throws UserAlreadyExistsException Si el email o username ya existen.
     */
    public User register(RegisterRequest dto) throws IOException { 
        List<User> users = persistenceManager.readCollection(USERS_FILE, User.class);

        // 1. Validar duplicados
        if (users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(dto.getEmail()))) {
            throw new UserAlreadyExistsException("El email " + dto.getEmail() + " ya está en uso.");
        }
        if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(dto.getUsername()))) {
            throw new UserAlreadyExistsException("El usuario " + dto.getUsername() + " ya está en uso.");
        }

        // 2. Hashear password
        String hashedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());

        // 3. Crear Preferencias a partir del DTO
        PreferenciasUsuario prefs = new PreferenciasUsuario();
        // Asignar preferencias de búsqueda si vienen en el DTO
        prefs.setBusquedaJuegoPorDefecto(dto.getJuegoPreferido());
        prefs.setBusquedaRegionPorDefecto(dto.getRegionPreferida());
        prefs.setBusquedaRangoMinPorDefecto(dto.getRangoMinPreferido());
        prefs.setBusquedaRangoMaxPorDefecto(dto.getRangoMaxPreferido());
        // Establecer canales (asegurando EMAIL)
        if (dto.getCanalesPreferidos() != null && !dto.getCanalesPreferidos().isEmpty()) {
             Set<String> validChannels = dto.getCanalesPreferidos().stream()
                 .filter(c -> {
                     try {
                         CanalNotificacion.valueOf(c.toUpperCase());
                         return true;
                     } catch (IllegalArgumentException | NullPointerException e) { 
                         logger.warn("Canal de notificación inválido '{}' ignorado durante registro.", c);
                         return false;
                     }
                 })
                 .map(String::toUpperCase)
                 .collect(Collectors.toSet());
            prefs.setCanalesNotificacion(validChannels);
        }
        if (prefs.getCanalesNotificacion().isEmpty()) {
            prefs.getCanalesNotificacion().add(CanalNotificacion.EMAIL.name());
             logger.info("No se especificaron canales de notificación válidos para {}, se usará EMAIL por defecto.", dto.getUsername());
        }
        // Dejar alertas activadas por defecto (el usuario las puede desactivar después en su perfil)
        prefs.setAlertasScrim(true);
        prefs.setAlertasPostulacion(true);
        prefs.setRecordatoriosActivos(true);

        // 4. Crear usuario
        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());
        newUser.setPasswordHash(hashedPassword);
        newUser.setRol(UserRole.USER);
        newUser.setEstadoVerificacion(VerificationState.PENDIENTE);
        newUser.setStrikes(0);
        newUser.setPreferencias(prefs); 

        // 5. Guardar usuario
        users.add(newUser);
        persistenceManager.writeCollection(USERS_FILE, users);
        logger.info("Usuario {} registrado con ID {}. Preferencias iniciales: Canales={}, Juego={}, Region={}",
                dto.getUsername(), newUser.getId(), prefs.getCanalesNotificacion(), prefs.getBusquedaJuegoPorDefecto(), prefs.getBusquedaRegionPorDefecto());

        // 6. Generar y guardar token de verificacion
        String verificationTokenString = generateNewToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);
        VerificationToken verificationToken = new VerificationToken(verificationTokenString, newUser.getId(), expiryDate);
        List<VerificationToken> tokens = persistenceManager.readCollection(VERIFICATIONS_FILE, VerificationToken.class);
        tokens.removeIf(t -> t.getExpiresAt().isBefore(LocalDateTime.now().minusDays(7)));
        tokens.add(verificationToken);
        persistenceManager.writeCollection(VERIFICATIONS_FILE, tokens);
        logger.info("Token de verificación generado para usuario {}", newUser.getId());

        // 7. Enviar notificacion con link
        String verificationLink = appBaseUrl + "/api/auth/verify?token=" + verificationTokenString;
        sendWelcomeEmailAsync(newUser, verificationLink);

        return newUser;
    }

    /**
     * Metodo para enviar el email de bienvenida/verificacion.
     * @param user El nuevo usuario.
     * @param verificationLink El link de verificación a incluir en el email.
     */
    @Async
    public void sendWelcomeEmailAsync(User user, String verificationLink) {
        try {
             Thread.sleep(200); 
        } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             logger.warn("Pausa interrumpida antes de enviar email de bienvenida a {}", user.getEmail());
        }
        notificationService.sendWelcomeNotification(user, verificationLink);
    }

    // Autentica un usuario y crea una sesion.
    public LoginResponse login(String email, String password) throws IOException {
        List<User> users = persistenceManager.readCollection(USERS_FILE, User.class);

        User user = users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException("Email o contraseña incorrectos."));

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Email o contraseña incorrectos.");
        }

        List<Session> sessions = persistenceManager.readCollection(SESSIONS_FILE, Session.class);
        String sessionToken = generateNewToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        Session newSession = new Session(sessionToken, user.getId(), expiresAt);

        sessions.removeIf(s -> s.getExpiresAt().isBefore(LocalDateTime.now()));
        sessions.add(newSession);
        persistenceManager.writeCollection(SESSIONS_FILE, sessions);
        logger.info("Sesión creada para usuario {}", user.getId());

        return new LoginResponse(newSession.getToken(), user);
    }

    //Verifica un email usando un token recibido.
    // Cambia el estado del usuario a VERIFICADO y elimina el token.

    public void verifyEmail(String tokenString) throws IOException {
        List<VerificationToken> tokens = persistenceManager.readCollection(VERIFICATIONS_FILE, VerificationToken.class);

        Optional<VerificationToken> tokenOpt = tokens.stream()
                .filter(t -> t.getToken().equals(tokenString))
                .findFirst();

        if (tokenOpt.isEmpty()) {
            logger.warn("Intento de verificación con token inválido: {}", tokenString);
            throw new InvalidTokenException("El token de verificación es inválido o ya fue utilizado.");
        }
        VerificationToken token = tokenOpt.get();

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Intento de verificación con token expirado para usuario {}: {}", token.getUserId(), tokenString);
            tokens.remove(token);
            persistenceManager.writeCollection(VERIFICATIONS_FILE, tokens); // Borrar token expirado
            throw new TokenExpiredException("El token de verificación ha expirado. Por favor, solicita uno nuevo.");
        }

        List<User> users = persistenceManager.readCollection(USERS_FILE, User.class);
        Optional<User> userOpt = users.stream().filter(u -> u.getId().equals(token.getUserId())).findFirst();

        if (userOpt.isEmpty()) {
            logger.error("CRITICAL: Token de verificación válido encontrado para usuario NO existente. UserID: {}, Token: {}", token.getUserId(), tokenString);
            tokens.remove(token); // Borrar token huerfano
            persistenceManager.writeCollection(VERIFICATIONS_FILE, tokens);
            throw new InvalidTokenException("Usuario asociado al token no encontrado.");
        }
        User user = userOpt.get();

        boolean userWasUpdated = false;
        if (user.getEstadoVerificacion() == VerificationState.PENDIENTE) {
            user.setEstadoVerificacion(VerificationState.VERIFICADO); 
            logger.info("Email verificado exitosamente para usuario {}", user.getId());

            // Actualizar la lista de usuarios para persistencia
            List<User> updatedUsers = users.stream()
                .map(u -> u.getId().equals(user.getId()) ? user : u) 
                .collect(Collectors.toList());

            persistenceManager.writeCollection(USERS_FILE, updatedUsers); 
            userWasUpdated = true; 

        } else {
            logger.warn("Usuario {} ya estaba verificado. Token {} será eliminado.", user.getId(), tokenString);
        }

        // Eliminar token para que no se reuse
        boolean removed = tokens.remove(token);
        if (removed) {
             persistenceManager.writeCollection(VERIFICATIONS_FILE, tokens);
             logger.info("Token de verificación eliminado: {}", tokenString);
        } else {
             logger.warn("El token {} ya había sido eliminado (posible condición de carrera?).", tokenString);
        }

        if (user.getEstadoVerificacion() == VerificationState.VERIFICADO && !userWasUpdated && users.stream().anyMatch(u -> u.getId().equals(user.getId()) && u.getEstadoVerificacion() == VerificationState.PENDIENTE) ) {
             logger.error("CRITICAL: El estado del usuario {} no pudo ser actualizado a VERIFICADO en el archivo.", user.getId());
        }
    }

    // Genera un token aleatorio seguro

    private String generateNewToken() {
        byte[] randomBytes = new byte[32]; 
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}

