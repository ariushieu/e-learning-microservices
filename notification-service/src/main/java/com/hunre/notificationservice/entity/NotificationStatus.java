package com.hunre.notificationservice.entity;

/** Vòng đời một thông báo. Khớp ràng buộc CHECK trong V1__init_notification_schema.sql. */
public enum NotificationStatus {

    /** Đã tạo, chưa gửi đi. Thông báo IN_APP không dừng ở đây lâu. */
    PENDING,

    /** Đã gửi tới người dùng. */
    SENT,

    /** Gửi hỏng, xem cột last_error. */
    FAILED,

    /** Người dùng đã đọc. */
    READ
}
