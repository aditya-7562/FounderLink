package com.founderlink.auth.config;

import com.founderlink.auth.entity.Role;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.exception.AdminSeedingException;
import com.founderlink.auth.repository.UserRepository;
import com.founderlink.auth.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SyncService syncService;

    @Transactional
    public void seedAdmin(String name, String email, String rawPassword) {

        log.debug("Starting admin seeding process");

        if (userRepository.existsByEmail(email)) {
            log.debug("Admin already exists");
            return;
        }

        User admin = new User();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ADMIN);

        User saved;

        try {
            saved = userRepository.saveAndFlush(admin);
            log.debug("Admin persisted in auth-service");
        } catch (Exception e) {
            log.error("Failed to save admin in auth-service", e);
            throw new AdminSeedingException("Failed to save admin in auth-service", e);
        }

        try {
            syncService.syncUser(saved);
        } catch (Exception e) {
            log.error("Admin sync failed", e);
            throw new AdminSeedingException("Failed to sync admin with user-service", e);
        }

        log.debug("Admin seeding completed");
    }
}
