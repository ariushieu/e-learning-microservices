package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Giảng viên vừa đăng một thông báo cho học viên của khóa học.
 *
 * <p>Phát bởi course-service lên topic {@link KafkaTopics#COURSE_EVENTS}. notification-service
 * nghe sự kiện này và tạo thông báo theo mẫu {@code COURSE_ANNOUNCEMENT} cho từng người trong
 * {@link #recipientIds()}.
 *
 * <p>Danh sách người nhận được chốt ngay lúc đăng, lấy từ bảng {@code course_learners} của
 * course-service. Để notification-service tự đi hỏi enrollment-service thì một thông báo đăng
 * lúc 9 giờ có thể tới cả người ghi danh lúc 10 giờ, tùy consumer chạy chậm hay nhanh.
 *
 * <p>{@link #preview()} là đoạn đầu nội dung, đã cắt ngắn; nội dung đầy đủ ở trang khóa học.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseAnnouncementPostedEvent(
        String eventId,
        Instant occurredAt,
        Long announcementId,
        Long courseId,
        String courseTitle,
        String title,
        String preview,
        List<Long> recipientIds
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.COURSE_ANNOUNCEMENT_POSTED;
    }

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static CourseAnnouncementPostedEvent of(Long announcementId, Long courseId, String courseTitle,
                                                   String title, String preview, List<Long> recipientIds) {
        return new CourseAnnouncementPostedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                announcementId,
                courseId,
                courseTitle,
                title,
                preview,
                List.copyOf(recipientIds));
    }
}
