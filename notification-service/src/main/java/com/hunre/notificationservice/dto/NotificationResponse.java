package com.hunre.notificationservice.dto;

import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationStatus;

import java.time.Instant;

/**
 * Một thông báo như frontend nhìn thấy.
 *
 * <p>Không trả {@code retryCount} và {@code lastError}: đó là chi tiết vận hành của việc
 * gửi, người dùng không cần biết và cũng không nên thấy thông báo lỗi kỹ thuật.
 */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String content,
        String linkUrl,
        boolean read,
        Instant createdAt,
        Instant readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getLinkUrl(),
                notification.getStatus() == NotificationStatus.READ,
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
