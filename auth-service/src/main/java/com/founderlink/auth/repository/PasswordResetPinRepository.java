package com.founderlink.auth.repository;

import com.founderlink.auth.entity.PasswordResetPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetPinRepository extends JpaRepository<PasswordResetPin, Long> {
    Optional<PasswordResetPin> findByEmailAndPin(String email, String pin);
    void deleteByEmail(String email);
}
