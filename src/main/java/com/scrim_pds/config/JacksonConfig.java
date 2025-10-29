package com.scrim_pds.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración de Jackson (el parser de JSON).
 * Versión Manual y Explícita.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Módulo para Fechas (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        // 2. No escribir fechas como números (Timestamps)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 3. Escribir JSON bonito (Indentado)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 4. --- LA CLAVE QUE FALTABA EN ETAPA 1 ---
        // Habilita la LECTURA (Deserialización) de Enums a partir de su nombre (String)
        // Esto convierte "FORMATO_5V5" (JSON) -> Formato.FORMATO_5V5 (Java)
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING); 
            
        // 5. --- LA OTRA CLAVE ---
        // Habilita la ESCRITURA (Serialización) de Enums a su nombre (String)
        // Esto convierte Formato.FORMATO_5V5 (Java) -> "FORMATO_5V5" (JSON)
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING); 

        // 6. (Buena práctica) No fallar si el JSON tiene campos extra que el DTO no tiene
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}

