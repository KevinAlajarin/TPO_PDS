package com.scrim_pds.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Configuración de Jackson (el parser de JSON).

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Modulo para Fechas (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        // 2. No escribir fechas como numeros (Timestamps)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 3. Escribir JSON bonito (Indentado)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 4. 
        // Habilita la LECTURA (Deserialización) de Enums a partir de su nombre (String)
        // Esto convierte "FORMATO_5V5" (JSON) -> Formato.FORMATO_5V5 (Java)
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING); 
            
        // 5.
        // Habilita la ESCRITURA (Serializacion) de Enums a su nombre (String)
        // Esto convierte Formato.FORMATO_5V5 (Java) -> "FORMATO_5V5" (JSON)
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING); 

        // 6. (Buena practica) No fallar si el JSON tiene campos extra que el DTO no tiene
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}

