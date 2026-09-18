package com.hunre.authservice.repository;

import com.hunre.authservice.domain.VerificationToken;
import com.hunre.authservice.domain.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, VerificationTokenType type);
}
