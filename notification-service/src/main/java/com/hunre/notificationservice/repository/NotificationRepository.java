package com.hunre.notificationservice.repository;

import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Tìm theo cả id lẫn userId để không ai đọc được thông báo của người khác. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatusNot(Long userId, NotificationStatus status);

    /**
     * Đánh dấu đã đọc mọi thông báo chưa đọc của một người. {@code readAt} cũ được giữ, giống
     * {@code Notification.markRead()}. Câu UPDATE hàng loạt bỏ qua {@code @UpdateTimestamp}
     * nên phải tự đặt {@code updatedAt}.
     *
     * @return số dòng đã đổi
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n
               set n.status = :read, n.readAt = coalesce(n.readAt, :now), n.updatedAt = :now
             where n.userId = :userId and n.status <> :read
            """)
    int markAllRead(@Param("userId") Long userId,
                    @Param("read") NotificationStatus read,
                    @Param("now") Instant now);
}
