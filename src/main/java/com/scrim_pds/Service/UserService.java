package com.scrim_pds.service;

import com.scrim_pds.model.Session;
import com.scrim_pds.model.User;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.slf4j.Logger; // <-- AÑADIR IMPORT
import org.slf4j.LoggerFactory; // <-- AÑADIR IMPORT
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
