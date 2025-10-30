package com.scrim_pds.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrim_pds.dto.LoginRequest;
import com.scrim_pds.dto.LoginResponse;
import com.scrim_pds.dto.RegisterRequest;
import com.scrim_pds.exception.InvalidCredentialsException;
import com.scrim_pds.exception.InvalidTokenException;
import com.scrim_pds.exception.TokenExpiredException;
import com.scrim_pds.exception.UserAlreadyExistsException;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.UserRole; 
import com.scrim_pds.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // --- Tests de REGISTER ---

    @Test
    void registerUser_shouldReturnCreated_whenDataIsValid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integrationTestUser");
        request.setEmail("integration@test.com");
        request.setPassword("validPassword123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente. Por favor verifica tu email."))
                .andExpect(jsonPath("$.userId").value(mockUser.getId().toString()));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenDataIsInvalid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("u"); // Invalido
        request.setEmail("not-an-email");
        request.setPassword("short");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos de entrada inválidos"));
        
        verify(authService, never()).register(any());
    }

     @Test
    void registerUser_shouldReturnConflict_whenEmailExists() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("existing@test.com");
        request.setPassword("validPassword123");

        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new UserAlreadyExistsException("El email existing@test.com ya está en uso."));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El email existing@test.com ya está en uso."));
    }

    // --- TESTS PARA LOGIN y VERIFY ---

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // Arrange
        String email = "test@test.com";
        String token = "mock-session-token";

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail(email);
        mockUser.setRol(UserRole.USER); 
        LoginResponse mockResponse = new LoginResponse(token, mockUser);

        when(authService.login(eq(email), anyString())).thenReturn(mockResponse);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsAreInvalid() throws Exception {
        // Arrange
        when(authService.login(anyString(), anyString()))
            .thenThrow(new InvalidCredentialsException("Email o contraseña incorrectos."));
        
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrongpass");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contraseña incorrectos."));
    }

    @Test
    void verifyEmail_shouldReturnOk_whenTokenIsValid() throws Exception {
        // Arrange
        String validToken = "valid-token";
        doNothing().when(authService).verifyEmail(eq(validToken));

        // Act & Assert
        mockMvc.perform(get("/api/auth/verify")
                .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Email verificado correctamente."));
    }

    @Test
    void verifyEmail_shouldReturnBadRequest_whenTokenIsInvalid() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";
        doThrow(new InvalidTokenException("El token de verificación es inválido."))
            .when(authService).verifyEmail(eq(invalidToken));

        // Act & Assert
        mockMvc.perform(get("/api/auth/verify")
                .param("token", invalidToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El token de verificación es inválido."));
    }

    @Test
    void verifyEmail_shouldReturnBadRequest_whenTokenIsExpired() throws Exception {
        // Arrange
        String expiredToken = "expired-token";
        doThrow(new TokenExpiredException("El token de verificación ha expirado."))
            .when(authService).verifyEmail(eq(expiredToken));
        
        // Act & Assert
        mockMvc.perform(get("/api/auth/verify")
                .param("token", expiredToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El token de verificación ha expirado."));
    }
}

