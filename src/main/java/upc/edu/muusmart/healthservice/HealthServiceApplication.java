package upc.edu.muusmart.healthservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Health Management microservice.
 *
 * <p>This microservice handles the registration and retrieval of animal health records. It
 * leverages Spring Boot for rapid development and runs an embedded server.</p>
 */
@SpringBootApplication
public class HealthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthServiceApplication.class, args);
    }
}