package com.scrim_pds.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrim_pds.dto.ScrimCreateRequest;
import com.scrim_pds.model.Postulacion;
import com.scrim_pds.model.Scrim;
import com.scrim_pds.model.User;
import com.scrim_pds.model.enums.Formato;
import com.scrim_pds.model.enums.MatchmakingStrategyType;
import com.scrim_pds.model.enums.Modalidad;
import com.scrim_pds.model.enums.ScrimStateEnum;
import com.scrim_pds.service.ScrimService;
import com.scrim_pds.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ScrimControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Mockeamos TODOS los servicios que usa este controlador ---
    @MockBean
    private ScrimService scrimService;

    // --- IMPORTANTE: Mockear UserService para simular @AuthUser ---
    @MockBean
    private UserService userService;

    // --- Datos de simulacion ---
    private User mockUser;
    private String mockToken;
    private Scrim mockScrim;
    private UUID mockScrimId;

    @BeforeEach
    void setUp() {
        // 1. Configurar el usuario simulado
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("mockOrganizador");
        mockUser.setEmail("mock@test.com");
        
        // 2. Configurar el token simulado
        mockToken = "mock-bearer-token";

        // 3. Configurar el mock de UserService para que devuelva al usuario cuando
        //    el AuthUserArgumentResolver busque el token.
        when(userService.findUserByToken(eq(mockToken))).thenReturn(Optional.of(mockUser));

        // 4. Configurar un Scrim simulado
        mockScrimId = UUID.randomUUID();
        mockScrim = new Scrim();
        mockScrim.setId(mockScrimId);
        mockScrim.setJuego("Valorant");
        mockScrim.setOrganizadorId(mockUser.getId());
        mockScrim.setEstado(ScrimStateEnum.BUSCANDO);
    }

    // --- Test 1: GET /api/scrims ---
    @Test
    void getScrims_shouldReturnOkAndEmptyList_whenNoFilters() throws Exception {
        // Arrange
        // Simular que el servicio no encuentra scrims
        when(scrimService.findScrims(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // Act & Assert
        mockMvc.perform(get("/api/scrims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- Test 2: POST /api/scrims (Exito) ---
    @Test
    void createScrim_shouldReturnCreated_whenAuthenticatedAndDataIsValid() throws Exception {
        // Arrange
        ScrimCreateRequest request = new ScrimCreateRequest(); // DTO válido
        request.setJuego("Valorant");
        request.setFormato(Formato.FORMATO_5V5);
        request.setRegion("LATAM");
        request.setRangoMin("Oro");
        request.setRangoMax("Platino");
        request.setLatenciaMax(100);
        request.setFechaHora(LocalDateTime.now().plusDays(1));
        request.setDuracion(60);
        request.setModalidad(Modalidad.RANKED);
        request.setCupo(10);
        request.setMatchmakingStrategyType(MatchmakingStrategyType.BY_MMR);

        // Simular que el servicio crea el scrim
        when(scrimService.createScrim(any(ScrimCreateRequest.class), eq(mockUser)))
            .thenReturn(mockScrim); // Devuelve el scrim simulado

        // Act & Assert
        mockMvc.perform(post("/api/scrims")
                .header("Authorization", "Bearer " + mockToken) // <-- Autenticación
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // 201
                .andExpect(jsonPath("$.id").value(mockScrimId.toString()))
                .andExpect(jsonPath("$.juego").value("Valorant"));
    }

    // --- Test 3: POST /api/scrims (Sin Auth) ---
    @Test
    void createScrim_shouldReturnUnauthorized_whenTokenIsMissing() throws Exception {
        // Arrange
        ScrimCreateRequest request = new ScrimCreateRequest(); // DTO válido (mismo que arriba)
        request.setJuego("Valorant");
        request.setFormato(Formato.FORMATO_5V5);
        // ... (resto de campos no importan tanto)
        request.setFechaHora(LocalDateTime.now().plusDays(1));
        request.setModalidad(Modalidad.RANKED);
        request.setCupo(10);
        request.setMatchmakingStrategyType(MatchmakingStrategyType.BY_MMR);
        request.setRangoMin("Oro");
        request.setRangoMax("Platino");
        request.setLatenciaMax(100);
        request.setDuracion(60);
        request.setRegion("LATAM");

        // Act & Assert
        mockMvc.perform(post("/api/scrims")
                // SIN header "Authorization"
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.error").value("Header 'Authorization: Bearer <token>' faltante o mal formado."));
    }

    // --- Test 4: POST /api/scrims/{id}/postulaciones ---
    @Test
    void postularseAScrim_shouldReturnCreated_whenAuthenticated() throws Exception {
        // Arrange
        Postulacion mockPostulacion = new Postulacion();
        mockPostulacion.setId(UUID.randomUUID());
        mockPostulacion.setUsuarioId(mockUser.getId());
        mockPostulacion.setScrimId(mockScrimId);

        when(scrimService.postularse(eq(mockScrimId), any(), eq(mockUser)))
            .thenReturn(mockPostulacion);

        String requestBody = "{ \"rolDeseado\": \"Duelista\", \"latenciaReportada\": 50 }";

        // Act & Assert
        mockMvc.perform(post("/api/scrims/{id}/postulaciones", mockScrimId)
                .header("Authorization", "Bearer " + mockToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    // --- Test 5: POST /api/scrims/{id}/finalizar ---
    @Test
    void finalizarScrim_shouldReturnOk_whenAuthenticated() throws Exception {
        // Arrange
        // Simular que el servicio (que es void) no lanza error
        doNothing().when(scrimService).finalizarScrim(eq(mockScrimId), eq(mockUser));
        
        // Act & Assert
        mockMvc.perform(post("/api/scrims/{id}/finalizar", mockScrimId)
                .header("Authorization", "Bearer " + mockToken))
                .andExpect(status().isOk());
    }

    // TODO: Añadir tests para /iniciar, /cancelar, /confirmaciones, /estadisticas
}

