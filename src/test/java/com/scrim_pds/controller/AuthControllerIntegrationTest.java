package com.scrim_pds.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrim_pds.dto.RegisterRequest;
import com.scrim_pds.model.User;
import com.scrim_pds.persistence.JsonPersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean; // Usamos MockBean para reemplazar el bean real
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.doAnswer; // <-- ¡AÑADIR ESTA LÍNEA!

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // Carga el contexto completo de Spring Boot
@AutoConfigureMockMvc // Configura MockMvc para simular peticiones HTTP
class AuthControllerIntegrationTest {

    @Autowired // Inyecta la instancia real de MockMvc
    private MockMvc mockMvc;

    @Autowired // Inyecta el ObjectMapper configurado por Spring
    private ObjectMapper objectMapper;

    @MockBean // Reemplaza el bean JsonPersistenceManager real por un Mock
    private JsonPersistenceManager persistenceManager;

    private List<User> userList;

    @BeforeEach
    void setUp() throws IOException { // @MockBean requiere manejo de IOException si el método original lo tiene
        userList = new ArrayList<>();
        // Configuración básica del mock para que no falle al leer
        when(persistenceManager.readCollection(eq("users.json"), eq(User.class))).thenReturn(userList);
        when(persistenceManager.readCollection(eq("sessions.json"), any())).thenReturn(new ArrayList<>()); // Para login si lo testeamos
        // Mockear writeCollection para evitar escrituras reales
        // doNothing().when(persistenceManager).writeCollection(anyString(), any());
        // O mockearlo para que simule añadir a la lista
         doAnswer(invocation -> {
             userList.addAll((List<User>) invocation.getArgument(1));
             return null;
         }).when(persistenceManager).writeCollection(eq("users.json"), any());
    }

    @Test
    void registerUser_shouldReturnCreated_whenDataIsValid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integrationTestUser");
        request.setEmail("integration@test.com");
        request.setPassword("validPassword123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register") // Simula un POST
                .contentType(MediaType.APPLICATION_JSON) // Pone el Content-Type
                .content(objectMapper.writeValueAsString(request))) // Convierte el DTO a JSON String
                .andExpect(status().isCreated()) // Verifica que el status HTTP sea 201
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente.")) // Verifica parte del JSON de respuesta
                .andExpect(jsonPath("$.userId").exists()); // Verifica que el userId exista
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenDataIsInvalid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("u"); // Inválido (muy corto)
        request.setEmail("not-an-email"); // Inválido
        request.setPassword("short"); // Inválido

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Verifica status 400
                .andExpect(jsonPath("$.error").value("Datos de entrada inválidos"))
                .andExpect(jsonPath("$.detalles.username").exists()) // Verifica que hay error de username
                .andExpect(jsonPath("$.detalles.email").exists()) // Verifica error de email
                .andExpect(jsonPath("$.detalles.password").exists()); // Verifica error de password
    }

     @Test
    void registerUser_shouldReturnConflict_whenEmailExists() throws Exception {
        // Arrange
        User existingUser = new User();
        existingUser.setEmail("existing@test.com");
        userList.add(existingUser); // Simular que el email ya existe en la "base de datos" mockeada

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("existing@test.com"); // Email duplicado
        request.setPassword("validPassword123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) // Verifica status 409
                .andExpect(jsonPath("$.error").value("El email existing@test.com ya está en uso."));
    }

    // TODO: Añadir tests de integración para /api/auth/login
}