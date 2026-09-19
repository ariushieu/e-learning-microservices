package com.hunre.notificationservice.repository;

import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Tìm theo cả id lẫn userId để không ai đọc được thông báo của người khác. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatusNot(Long userId, NotificationStatus status);
}
