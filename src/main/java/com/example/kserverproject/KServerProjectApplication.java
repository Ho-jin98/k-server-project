package com.example.kserverproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KServerProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(KServerProjectApplication.class, args);
    }

}
