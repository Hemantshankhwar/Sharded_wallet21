package com.example.sharded_wallet21.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.sharded_wallet21.entities.Role;
import com.example.sharded_wallet21.repositories.UserRepository;
import com.example.sharded_wallet21.services.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("No admin exists and ADMIN_EMAIL/ADMIN_PASSWORD are not set; nobody can administer the system");
            return;
        }
        authService.createUser("Administrator", adminEmail, adminPassword, Role.ADMIN);
        log.info("Bootstrap admin created for {}", adminEmail);
    }
}
