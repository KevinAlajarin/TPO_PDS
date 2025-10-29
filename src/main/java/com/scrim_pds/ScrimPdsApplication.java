package com.scrim_pds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ScrimPdsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrimPdsApplication.class, args);
    }

}