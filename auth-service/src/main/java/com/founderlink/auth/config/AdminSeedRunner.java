package com.founderlink.auth.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

    private final AdminSeeder adminSeeder;

    @Value("${seed.admin.enabled:false}")
    private boolean seedAdmin;

    @Value("${seed.admin.name:Super Admin}")
    private String adminName;

    @Value("${seed.admin.email:admin@founderlink.com}")
    private String adminEmail;

    @Value("${seed.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        log.info("Admin seed runner started enabled={} email={}", seedAdmin, adminEmail);

        if (!seedAdmin) {
            log.info("Admin seeding is disabled");
            return;
        }

        log.info("Admin seeding is ENABLED for email={}", adminEmail);

        if (adminPassword == null || adminPassword.isBlank()) {
            log.error("Admin password is missing while seeding is enabled");
            return;
        }

        if (!isPasswordValid(adminPassword)) {
            log.error("Admin password validation failed for email={}. Startup will continue without seeding.", adminEmail);
            return;
        }

        try {
            log.info("Starting admin seeding for email={}", adminEmail);
            adminSeeder.seedAdmin(adminName, adminEmail, adminPassword);
            log.info("Admin seeding process finished for email={}", adminEmail);

        } catch (Exception e) {
            log.error("Admin seeding failed for email={}. Startup will continue.", adminEmail, e);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit);
    }
}
