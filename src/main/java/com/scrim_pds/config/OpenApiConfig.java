package com.scrim_pds.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "eScrim API", version = "v1", description = "API para la plataforma de organización de Scrims"))
@SecurityScheme( // Define el esquema de seguridad Bearer JWT (aunque usemos token simple)
    name = "bearerAuth", // Nombre que usaremos en @SecurityRequirement
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT", // O simplemente "token"
    scheme = "bearer"
)

public class OpenApiConfig {
}
