package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Người học vừa ghi danh thành công một khóa học.
 *
 * <p>Phát bởi enrollment-service lên topic {@link KafkaTopics#ENROLLMENT_EVENTS}.
 * notification-service nghe sự kiện này và gửi thông báo theo mẫu
 * {@code ENROLLMENT_SUCCESS}; course-service nghe để tăng {@code courses.student_count}.
 *
 * <p>Sự kiện chỉ mang dữ liệu mà enrollment-service thật sự sở hữu: các id và tên khóa
 * học (lấy từ bảng {@code course_snapshots}). Thông tin người dùng như email hay họ tên
 * thuộc về auth-service, nên consumer nào cần thì tự lấy, không nhét vào đây. Nhét vào
 * đồng nghĩa enrollment-service phải gọi sang auth-service ngay lúc ghi danh, biến một
 * luồng bất đồng bộ thành phụ thuộc đồng bộ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentCreatedEvent(
        String eventId,
        Instant occurredAt,
        Long enrollmentId,
        Long userId,
        Long courseId,
        String courseTitle
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.ENROLLMENT_CREATED;
    }

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static EnrollmentCreatedEvent of(
            Long enrollmentId, Long userId, Long courseId, String courseTitle) {

        return new EnrollmentCreatedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                enrollmentId,
                userId,
                courseId,
                courseTitle);
    }
}
