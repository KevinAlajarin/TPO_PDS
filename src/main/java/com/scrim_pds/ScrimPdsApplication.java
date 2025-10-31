package com.scrim_pds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableAsync // Para emails
@EnableScheduling 
@EnableRetry
public class ScrimPdsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrimPdsApplication.class, args);
    }

}

