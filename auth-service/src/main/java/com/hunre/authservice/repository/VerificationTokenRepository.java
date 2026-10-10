package com.hunre.authservice.repository;

import com.hunre.authservice.domain.VerificationToken;
import com.hunre.authservice.domain.VerificationTokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, VerificationTokenType type);

    /** Khóa dòng để hai request dùng cùng một mã không cùng đặt lại được mật khẩu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM VerificationToken t JOIN FETCH t.user WHERE t.tokenHash = :hash AND t.type = :type")
    Optional<VerificationToken> findForUpdate(@Param("hash") String tokenHash, @Param("type") VerificationTokenType type);

    List<VerificationToken> findByUserIdAndTypeAndUsedAtIsNull(Long userId, VerificationTokenType type);
}
