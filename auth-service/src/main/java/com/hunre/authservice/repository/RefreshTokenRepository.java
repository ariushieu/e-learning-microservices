package com.hunre.authservice.repository;

import com.hunre.authservice.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            Long userId, Instant now);

    Optional<RefreshToken> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user.id = :userId "
            + "AND r.id <> :sessionId AND r.revokedAt IS NULL AND r.expiresAt > :now")
    int revokeOtherSessions(@Param("userId") Long userId, @Param("sessionId") Long sessionId,
                            @Param("now") Instant now);
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("SELECT r.user.id FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :revokedAt WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    int revokeAllUserTokens(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);
}
