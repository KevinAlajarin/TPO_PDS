package com.scrim_pds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- AÑADIR IMPORT

@SpringBootApplication
@EnableAsync // Para emails
@EnableScheduling // <-- AÑADIR ESTA ANOTACIÓN
public class ScrimPdsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrimPdsApplication.class, args);
    }

}

