package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.LiveSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {
    List<LiveSession> findTop50ByCourseIdOrderByScheduledAtDescIdDesc(Long courseId);

    /** Khóa hàng khi đổi trạng thái: hai tab cùng bấm "Bắt đầu" thì chỉ một sự kiện được phát. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LiveSession s where s.id = :id")
    Optional<LiveSession> findForUpdate(@Param("id") Long id);
}
