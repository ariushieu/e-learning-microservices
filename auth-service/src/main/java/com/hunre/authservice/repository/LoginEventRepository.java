package com.hunre.authservice.repository;

import com.hunre.authservice.domain.LoginEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    Page<LoginEvent> findByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant cutoff, Pageable pageable);

    List<LoginEvent> findTop2ByUserIdAndSuccessTrueOrderByIdDesc(Long userId);

    long countByUserIdAndSuccessFalseAndIdGreaterThanAndIdLessThanAndCreatedAtGreaterThanEqual(
            Long userId, Long after, Long before, Instant cutoff);

    @Modifying
    @Query("DELETE FROM LoginEvent e WHERE e.createdAt < :cutoff")
    int deleteExpired(Instant cutoff);
}
