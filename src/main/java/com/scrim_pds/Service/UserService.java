package com.scrim_pds.service;

import com.scrim_pds.dto.PreferencesUpdateRequest; // <-- Importar DTO
import com.scrim_pds.dto.ProfileUpdateRequest; // <-- Importar DTO
import com.scrim_pds.exception.UserAlreadyExistsException; // <-- Importar Excepción
import com.scrim_pds.model.PreferenciasUsuario; // <-- Importar Preferencias
import com.scrim_pds.model.Session;
import com.scrim_pds.model.User;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
// import java.util.stream.Collectors; // No es necesario para esta clase

@Service
public class UserService {

    // AÑADIR LOGGER
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final JsonPersistenceManager persistenceManager;
    private final String USERS_FILE = "users.json";
    private final String SESSIONS_FILE = "sessions.json";

    public UserService(JsonPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    /**
     * Busca un usuario por su ID.
     * --- AHORA MANEJA IOException ---
     */
    public Optional<User> findUserById(UUID id) { // <-- QUITAR 'throws IOException'
        try {
            List<User> users = persistenceManager.readCollection(USERS_FILE, User.class);
            return users.stream().filter(u -> u.getId().equals(id)).findFirst();
        } catch (IOException e) {
            logger.error("Error al leer el archivo de usuarios (users.json): {}", e.getMessage());
            return Optional.empty(); // Si no se puede leer el archivo, no se puede encontrar al usuario.
        }
    }
    
    /**
     * Busca un usuario válido (no expirado) a partir de un token de sesión.
     * --- AHORA MANEJA IOException ---
     */
    public Optional<User> findUserByToken(String token) { // <-- QUITAR 'throws IOException'
        try {
            List<Session> sessions = persistenceManager.readCollection(SESSIONS_FILE, Session.class);
            
            Optional<Session> validSession = sessions.stream()
                    .filter(s -> s.getToken().equals(token) && s.getExpiresAt().isAfter(LocalDateTime.now()))
                    .findFirst();
            
            if (validSession.isEmpty()) {
                return Optional.empty();
            }
            
            // Esto ahora tampoco lanza IOException
            return findUserById(validSession.get().getUserId());

        } catch (IOException e) {
            logger.error("Error al leer el archivo de sesiones (sessions.json): {}", e.getMessage());
            return Optional.empty(); // Si no se puede leer el archivo, el token no es válido.
        }
    }


    // --- NUEVO MÉTODO AÑADIDO ---
    /**
     * Actualiza el perfil público de un usuario.
     * @param userId ID del usuario autenticado.
     * @param dto DTO con los datos a actualizar.
     * @return El usuario actualizado.
     * @throws IOException Si falla la persistencia.
     * @throws UserAlreadyExistsException Si el nuevo username ya está en uso.
     */
    public User updateUserProfile(UUID userId, ProfileUpdateRequest dto) throws IOException {
        List<User> users = persistenceManager.readCollection(USERS_FILE, User.class);

        // 1. Validar conflicto de username
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            boolean conflict = users.stream()
                    .anyMatch(u -> !u.getId().equals(userId) && u.getUsername().equalsIgnoreCase(dto.getUsername()));
            if (conflict) {
                throw new UserAlreadyExistsException("El nombre de usuario '" + dto.getUsername() + "' ya está en uso por otro usuario.");
            }
        }

        // 2. Encontrar y actualizar usuario
        Optional<User> userOpt = users.stream().filter(u -> u.getId().equals(userId)).findFirst();
        if (userOpt.isEmpty()) {
            // Esto no debería pasar si el token es válido, pero es un buen control
            logger.error("CRITICAL: Usuario autenticado con ID {} no encontrado en {}.", userId, USERS_FILE);
            // Lanzamos una excepción genérica porque esto es un error grave de estado
            throw new RuntimeException("Usuario autenticado no encontrado en la base de datos.");
        }

        User userToUpdate = userOpt.get();
        // Actualizar los campos (el objeto userToUpdate está DENTRO de la lista 'users')
        userToUpdate.setUsername(dto.getUsername());
        userToUpdate.setRegion(dto.getRegion());
        userToUpdate.setRangoPorJuego(dto.getRangoPorJuego()); // Sobrescribe el mapa completo
        userToUpdate.setRolesPreferidos(dto.getRolesPreferidos()); // Sobrescribe la lista completa

        // 3. Guardar la lista COMPLETA de usuarios (que ahora contiene el usuario modificado)
        persistenceManager.writeCollection(USERS_FILE, users);
        logger.info("Perfil actualizado para usuario {}", userId);

        return userToUpdate;
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    /**
     * Actualiza las preferencias de un usuario.
     * @param userId ID del usuario autenticado.
     * @param dto DTO con las preferencias a actualizar.
     * @return Las preferencias actualizadas.
     * @throws IOException Si falla la persistencia.
     */
    public PreferenciasUsuario updateUserPreferences(UUID userId, PreferencesUpdateRequest dto) throws IOException {
        List<User> users = persistenceManager.readCollection(USERS_FILE, User.class);

        Optional<User> userOpt = users.stream().filter(u -> u.getId().equals(userId)).findFirst();
        if (userOpt.isEmpty()) {
            logger.error("CRITICAL: Usuario autenticado con ID {} no encontrado en {}.", userId, USERS_FILE);
            throw new RuntimeException("Usuario autenticado no encontrado en la base de datos.");
        }

        User userToUpdate = userOpt.get();

        // Obtener (o crear si es nulo) el objeto de preferencias
        PreferenciasUsuario prefs = userToUpdate.getPreferencias();
        if (prefs == null) {
            logger.warn("Usuario {} no tenía objeto de preferencias, creando uno nuevo.", userId);
            prefs = new PreferenciasUsuario();
            userToUpdate.setPreferencias(prefs);
        }

        // Actualizar todos los campos desde el DTO
        // (Los setters de PreferenciasUsuario se encargarán de manejar nulls si es necesario)
        prefs.setCanalesNotificacion(dto.getCanalesNotificacion());
        prefs.setAlertasScrim(dto.getAlertasScrim());
        prefs.setAlertasPostulacion(dto.getAlertasPostulacion());
        prefs.setRecordatoriosActivos(dto.getRecordatoriosActivos());
        prefs.setBusquedaJuegoPorDefecto(dto.getBusquedaJuegoPorDefecto());
        prefs.setBusquedaRegionPorDefecto(dto.getBusquedaRegionPorDefecto());
        prefs.setBusquedaRangoMinPorDefecto(dto.getBusquedaRangoMinPorDefecto());
        prefs.setBusquedaRangoMaxPorDefecto(dto.getBusquedaRangoMaxPorDefecto());

        // Guardar la lista COMPLETA de usuarios
        // (ya que 'prefs' es parte de 'userToUpdate', que es parte de 'users')
        persistenceManager.writeCollection(USERS_FILE, users);
        logger.info("Preferencias actualizadas para usuario {}", userId);

        return prefs;
    }
}

