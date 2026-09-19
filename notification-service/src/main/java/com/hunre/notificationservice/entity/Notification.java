package com.hunre.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Một thông báo đã dựng xong cho một người dùng.
 *
 * <p>{@code title} và {@code content} lưu nội dung <b>đã điền placeholder</b>, không lưu
 * tham chiếu tới mẫu. Nhờ vậy sửa lại mẫu về sau không làm thay đổi những thông báo đã gửi
 * đi — người dùng mở lại hộp thư vẫn thấy đúng câu chữ họ đã đọc hôm trước.
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_inbox", columnList = "user_id, status, created_at"),
                @Index(name = "idx_notifications_pending", columnList = "status, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** auth_db.users.id, không đặt khóa ngoại vì khác database. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Trùng với {@code notification_templates.code}, ví dụ {@code QUIZ_GRADED}. */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Đánh dấu đã đọc. Gọi nhiều lần cũng chỉ ghi nhận lần đầu. */
    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
        this.status = NotificationStatus.READ;
    }
}
