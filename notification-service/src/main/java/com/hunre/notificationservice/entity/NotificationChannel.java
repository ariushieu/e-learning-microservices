package com.hunre.notificationservice.entity;

/** Kênh gửi thông báo. Khớp ràng buộc CHECK trong V1__init_notification_schema.sql. */
public enum NotificationChannel {

    /** Hiển thị trong ứng dụng, đọc qua API. */
    IN_APP,

    /** Gửi qua email. Hiện chưa có máy chủ mail nên chưa xử lý, xem docs/notifications.md. */
    EMAIL
}
