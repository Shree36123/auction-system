package com.auction.config;

import com.auction.model.*;
import com.auction.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Seeds initial data: admin user, sample teams, and sample players.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               TeamRepository teamRepository,
                               PlayerRepository playerRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Only seed data if the database is empty
            if (userRepository.count() > 0) return;

            // --- Create Admin User ---
            User admin = new User("admin", passwordEncoder.encode("admin123"), "System Admin", UserRole.ADMIN);
            userRepository.save(admin);

            System.out.println("=== Initial data loaded ===");
            System.out.println("Admin login: admin / admin123");
        };
    }
}
