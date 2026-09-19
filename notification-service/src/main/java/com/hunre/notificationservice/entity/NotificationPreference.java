package com.hunre.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Tùy chọn nhận thông báo của một người dùng.
 *
 * <p>Không có bản ghi nghĩa là bật cả hai kênh. Chỉ khi người dùng tự tắt mới sinh ra một
 * dòng ở đây, nên bảng này luôn nhỏ hơn bảng người dùng rất nhiều.
 *
 * <p>Khóa chính là {@code user_id} lấy từ auth_db, không có cột id riêng: mỗi người dùng
 * chỉ có đúng một bộ tùy chọn.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
