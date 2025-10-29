package com.scrim_pds.service;

import com.scrim_pds.dto.RegisterRequest; // <-- AÑADIR IMPORT
import com.scrim_pds.exception.UserAlreadyExistsException;
import com.scrim_pds.model.User;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set; // Import Set for preferences

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JsonPersistenceManager persistenceManager;
    // Mock NotificationService as well, since AuthService now depends on it
    @Mock
    private com.scrim_pds.notification.NotificationService notificationService;


    @InjectMocks
    private AuthService authService;

    private List<User> userList;

    @BeforeEach
    void setUp() throws IOException { // Added throws IOException for readCollection mock
        userList = new ArrayList<>();
        // Mock readCollection to return our list for users.json
        when(persistenceManager.readCollection(eq("users.json"), eq(User.class))).thenReturn(userList);
        // Mock readCollection for verification tokens (needed by register)
        when(persistenceManager.readCollection(eq("verifications.json"), any())).thenReturn(new ArrayList<>());
        // Mock writeCollection for verification tokens (needed by register)
        // doNothing().when(persistenceManager).writeCollection(eq("verifications.json"), any());
        // Mock writeCollection for users.json to simulate adding
         doAnswer(invocation -> {
             userList.clear(); // Clear previous state
             userList.addAll((List<User>) invocation.getArgument(1));
             return null;
         }).when(persistenceManager).writeCollection(eq("users.json"), any());
    }

    @Test
    void register_shouldCreateUser_whenDataIsValid() throws IOException {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        // Set optional preferences for testing
        request.setJuegoPreferido("Valorant");
        request.setCanalesPreferidos(Set.of("EMAIL"));

        // Act
        // --- LLAMADA MODIFICADA ---
        User newUser = authService.register(request);

        // Assert
        assertNotNull(newUser);
        assertEquals("testuser", newUser.getUsername());
        assertEquals("test@example.com", newUser.getEmail());
        assertNotNull(newUser.getPasswordHash());
        assertNotEquals("password123", newUser.getPasswordHash());
        assertEquals(1, userList.size()); // Verify user was added to the list

        // Verify preferences were set
        assertNotNull(newUser.getPreferencias());
        assertEquals("Valorant", newUser.getPreferencias().getBusquedaJuegoPorDefecto());
        assertTrue(newUser.getPreferencias().getCanalesNotificacion().contains("EMAIL"));

        // Verify persistence calls
        verify(persistenceManager, times(1)).writeCollection(eq("users.json"), eq(userList));
        verify(persistenceManager, times(1)).writeCollection(eq("verifications.json"), any()); // Verify token save
        // Verify notification call (uses ArgumentCaptor if we need to check link)
        verify(notificationService, times(1)).sendWelcomeNotification(eq(newUser), anyString());
    }

    @Test
    void register_shouldThrowException_whenEmailExists() throws IOException {
        // Arrange
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        userList.add(existingUser); // Simulate email exists

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com"); // Duplicate email
        request.setPassword("password123");

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            // --- LLAMADA MODIFICADA ---
            authService.register(request);
        });

        assertEquals("El email test@example.com ya está en uso.", exception.getMessage());
        // Verify writeCollection was NEVER called for users or verifications
        verify(persistenceManager, never()).writeCollection(eq("users.json"), any());
        verify(persistenceManager, never()).writeCollection(eq("verifications.json"), any());
        // Verify notification was NEVER called
        verify(notificationService, never()).sendWelcomeNotification(any(), anyString());
    }

     @Test
    void register_shouldThrowException_whenUsernameExists() throws IOException {
        // Arrange
        User existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setEmail("other@example.com"); // Different email
        userList.add(existingUser); // Simulate username exists

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser"); // Duplicate username
        request.setEmail("new@example.com");
        request.setPassword("password123");

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
             // --- LLAMADA MODIFICADA ---
            authService.register(request);
        });

        assertEquals("El usuario testuser ya está en uso.", exception.getMessage());
        verify(persistenceManager, never()).writeCollection(eq("users.json"), any());
        verify(persistenceManager, never()).writeCollection(eq("verifications.json"), any());
        verify(notificationService, never()).sendWelcomeNotification(any(), anyString());
    }

    // TODO: Añadir tests para el método login (éxito, email no existe, pass incorrecta, no verificado si aplica)
    // TODO: Añadir tests para el método verifyEmail (éxito, token no existe, token expirado, usuario no existe)
}
