package com.hunre.courseservice.entity;

/** SCHEDULED → LIVE → ENDED; chỉ buổi chưa bắt đầu mới hủy được (CANCELLED). */
public enum LiveSessionStatus {
    SCHEDULED,
    LIVE,
    ENDED,
    CANCELLED
}
