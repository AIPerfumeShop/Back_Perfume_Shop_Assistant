package com.example.spring_boot_project_api;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class SpringBootProjectApiApplication {

	public static void main(String[] args) {
		// Load .env into System properties so Spring Boot can replace placeholders like ${DB_PORT}
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        // Pin the JVM default timezone so LocalDateTime.now() and analytics
        // day boundaries (atStartOfDay) all use the same clock.
        TimeZone.setDefault(TimeZone.getTimeZone(
                System.getProperty("APP_TIME_ZONE", "Asia/Phnom_Penh")));
		SpringApplication.run(SpringBootProjectApiApplication.class, args);
	}

}
