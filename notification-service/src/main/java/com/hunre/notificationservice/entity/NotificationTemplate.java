package com.hunre.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Mẫu nội dung thông báo, nạp sẵn bằng migration.
 *
 * <p>Để trong database chứ không viết thẳng trong code, vì sửa câu chữ là việc hay xảy ra
 * và không đáng phải build lại rồi triển khai lại cả service.
 *
 * <p>Placeholder viết dạng <code>{courseTitle}</code>, xem {@code TemplateRenderer}.
 */
@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_templates_code_channel",
                        columnNames = {"code", "channel"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ENROLLMENT_SUCCESS, COURSE_COMPLETED, QUIZ_GRADED, CERTIFICATE_ISSUED. */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "title_template", nullable = false, length = 255)
    private String titleTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    /** Tắt một mẫu để ngừng gửi loại thông báo đó mà không phải xóa dữ liệu. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
