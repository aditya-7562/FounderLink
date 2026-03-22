package com.founderlink.auth.repository;

import com.founderlink.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select refreshToken from RefreshToken refreshToken where refreshToken.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT rt FROM RefreshToken rt
    WHERE rt.userId = :userId
    AND rt.revoked = false
    ORDER BY rt.createdAt ASC
    LIMIT 1
""")
    Optional<RefreshToken> findOldestActiveToken(Long userId);


    long countByUserIdAndRevokedFalse(Long userId);
}
