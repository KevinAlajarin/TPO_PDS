package com.scrim_pds.service;

import com.scrim_pds.dto.LoginResponse;
import com.scrim_pds.dto.RegisterRequest;
import com.scrim_pds.exception.InvalidCredentialsException;
import com.scrim_pds.exception.InvalidTokenException;
import com.scrim_pds.exception.TokenExpiredException;
import com.scrim_pds.exception.UserAlreadyExistsException;
import com.scrim_pds.model.Session;
import com.scrim_pds.model.User;
import com.scrim_pds.model.VerificationToken;
import com.scrim_pds.model.enums.UserRole; // <-- Importar UserRole
import com.scrim_pds.model.enums.VerificationState;
import com.scrim_pds.notification.NotificationService;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings; // <-- Importar MockitoSettings
import org.mockito.quality.Strictness; // <-- Importar Strictness
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection; // <-- Importar Collection
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // <-- AÑADIDO: Ignorar stubs innecesarios
class AuthServiceTest {

    @Mock
    private JsonPersistenceManager persistenceManager;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    private List<User> userList;
    private List<VerificationToken> tokenList;
    private List<Session> sessionList;

    @BeforeEach
    void setUp() throws IOException {
        userList = new ArrayList<>();
        tokenList = new ArrayList<>();
        sessionList = new ArrayList<>();

        // Mock de lectura (devuelve la lista en memoria)
        when(persistenceManager.readCollection(eq("users.json"), eq(User.class))).thenReturn(userList);
        when(persistenceManager.readCollection(eq("verifications.json"), eq(VerificationToken.class))).thenReturn(tokenList);
        when(persistenceManager.readCollection(eq("sessions.json"), eq(Session.class))).thenReturn(sessionList);

        // --- MOCK DE ESCRITURA CORREGIDO ---
        // Simula la sobreescritura del archivo
        doAnswer(invocation -> {
             // 1. Copiar la colección que se pasa al método write
             Collection<User> usersToSave = new ArrayList<>((Collection<User>) invocation.getArgument(1));
             // 2. Limpiar la lista en memoria
             userList.clear();
             // 3. Añadir los nuevos elementos
             userList.addAll(usersToSave);
             return null;
         }).when(persistenceManager).writeCollection(eq("users.json"), any());

         doAnswer(invocation -> {
             Collection<VerificationToken> tokensToSave = new ArrayList<>((Collection<VerificationToken>) invocation.getArgument(1));
             tokenList.clear();
             tokenList.addAll(tokensToSave);
             return null;
         }).when(persistenceManager).writeCollection(eq("verifications.json"), any());

         doAnswer(invocation -> {
             Collection<Session> sessionsToSave = new ArrayList<>((Collection<Session>) invocation.getArgument(1));
             sessionList.clear();
             sessionList.addAll(sessionsToSave);
             return null;
         }).when(persistenceManager).writeCollection(eq("sessions.json"), any());
    }

    // --- Tests de REGISTER (sin cambios, pero ahora el mock de setUp funciona) ---
    @Test
    void register_shouldCreateUser_whenDataIsValid() throws IOException {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setJuegoPreferido("Valorant");
        request.setCanalesPreferidos(Set.of("EMAIL"));

        // Act
        User newUser = authService.register(request);

        // Assert
        assertNotNull(newUser);
        assertEquals("testuser", newUser.getUsername());
        assertEquals(1, userList.size()); // <-- Esta aserción ahora debe pasar
        assertEquals(1, tokenList.size());
        verify(persistenceManager, times(1)).writeCollection(eq("users.json"), any());
        verify(persistenceManager, times(1)).writeCollection(eq("verifications.json"), any());
        verify(notificationService, times(1)).sendWelcomeNotification(eq(newUser), anyString());
    }

    @Test
    void register_shouldThrowException_whenEmailExists() throws IOException {
        // ... (sin cambios)
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        userList.add(existingUser);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

     @Test
    void register_shouldThrowException_whenUsernameExists() throws IOException {
        // ... (sin cambios)
        User existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setEmail("other@example.com");
        userList.add(existingUser);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }


    // --- NUEVOS TESTS PARA LOGIN (CORREGIDOS) ---

    @Test
    void login_shouldReturnLoginResponse_whenCredentialsAreValid() throws IOException {
        // Arrange
        String email = "login@test.com";
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail(email);
        existingUser.setPasswordHash(hashedPassword);
        existingUser.setEstadoVerificacion(VerificationState.VERIFICADO);
        existingUser.setRol(UserRole.USER); // <-- CORRECCIÓN: Añadir Rol
        userList.add(existingUser);

        // Act
        LoginResponse response = authService.login(email, password);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(email, response.getUser().getEmail());
        assertEquals(UserRole.USER.name(), response.getUser().getRol()); // Verificar Rol
        assertEquals(1, sessionList.size());
        verify(persistenceManager, times(1)).writeCollection(eq("sessions.json"), any());
    }

    @Test
    void login_shouldThrowException_whenEmailNotFound() throws IOException {
        // ... (sin cambios)
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> {
            authService.login("noexiste@test.com", "password123");
        });
        assertEquals("Email o contraseña incorrectos.", ex.getMessage());
    }

    @Test
    void login_shouldThrowException_whenPasswordIsIncorrect() throws IOException {
        // Arrange
        String email = "login@test.com";
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail(email);
        existingUser.setPasswordHash(hashedPassword);
        existingUser.setEstadoVerificacion(VerificationState.VERIFICADO);
        existingUser.setRol(UserRole.USER); // <-- CORRECCIÓN: Añadir Rol (buena práctica)
        userList.add(existingUser);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(email, "passwordINCORRECTA"); // Pass incorrecta
        });
        assertEquals(0, sessionList.size()); // No se crearon sesiones
    }

    // --- NUEVOS TESTS PARA VERIFY_EMAIL (sin cambios) ---

    @Test
    void verifyEmail_shouldSetUserToVerified_whenTokenIsValid() throws IOException {
        // Arrange
        User pendingUser = new User();
        pendingUser.setId(UUID.randomUUID());
        pendingUser.setEstadoVerificacion(VerificationState.PENDIENTE);
        userList.add(pendingUser);

        String tokenString = "valid-token";
        VerificationToken token = new VerificationToken(tokenString, pendingUser.getId(), LocalDateTime.now().plusDays(1));
        tokenList.add(token);

        // Act
        authService.verifyEmail(tokenString);

        // Assert
        assertEquals(VerificationState.VERIFICADO, pendingUser.getEstadoVerificacion());
        assertEquals(0, tokenList.size());
        verify(persistenceManager, times(1)).writeCollection(eq("users.json"), any());
        verify(persistenceManager, times(1)).writeCollection(eq("verifications.json"), any());
    }

    @Test
    void verifyEmail_shouldThrowException_whenTokenIsInvalid() throws IOException {
        assertThrows(InvalidTokenException.class, () -> authService.verifyEmail("invalid-token"));
    }

    @Test
    void verifyEmail_shouldThrowException_whenTokenIsExpired() throws IOException {
        // Arrange
        User pendingUser = new User();
        pendingUser.setId(UUID.randomUUID());
        userList.add(pendingUser);

        String tokenString = "expired-token";
        VerificationToken token = new VerificationToken(tokenString, pendingUser.getId(), LocalDateTime.now().minusDays(1));
        tokenList.add(token);

        // Act & Assert
        assertThrows(TokenExpiredException.class, () -> authService.verifyEmail(tokenString));
        assertEquals(0, tokenList.size()); // Token expirado se borra
        verify(persistenceManager, times(1)).writeCollection(eq("verifications.json"), any());
    }
}

