package com.hunre.authservice.repository;

import com.hunre.authservice.domain.User;
import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.domain.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    interface RoleCount {
        RoleCode getRole();
        long getTotal();
    }

    interface StatusCount {
        UserStatus getStatus();
        long getTotal();
    }

    @Query("SELECT r.code AS role, COUNT(u.id) AS total FROM User u JOIN u.roles r GROUP BY r.code")
    List<RoleCount> countByRole();

    @Query("SELECT u.status AS status, COUNT(u.id) AS total FROM User u GROUP BY u.status")
    List<StatusCount> countByStatus();

    @Query("SELECT COUNT(u.id) FROM User u WHERE u.createdAt >= :since AND u.createdAt <= :now")
    long countCreatedBetween(@Param("since") Instant since, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
}
