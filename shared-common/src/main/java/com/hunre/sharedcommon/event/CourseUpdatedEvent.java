package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái hiện tại của một khóa học mà service khác được phép biết.
 *
 * <p>Phát bởi course-service lên topic {@link KafkaTopics#COURSE_EVENTS}.
 * enrollment-service nghe để ghi hoặc cập nhật một dòng trong {@code course_snapshots} —
 * bản sao chỉ đọc giúp nó kiểm "khóa học có tồn tại và đang mở không" mà không phải đọc
 * {@code course_db}.
 *
 * <p><b>Đây là ảnh chụp, không phải mô tả thay đổi.</b> Mỗi sự kiện mang <b>toàn bộ</b>
 * các trường bên dưới với giá trị hiện tại, kể cả trường không đổi. Consumer chỉ việc ghi
 * đè cả dòng theo {@code courseId}, không phải so sánh xem trường nào mới. Nhờ vậy nhận
 * trùng hay bỏ lỡ một sự kiện ở giữa cũng không sao: sự kiện sau luôn đưa bản sao về đúng.
 *
 * <p><b>Vì sao không tách thành {@code course.published}.</b> Nếu chỉ báo lúc xuất bản thì
 * lưu trữ khóa học sẽ không phát gì, bản sao vẫn ghi {@code PUBLISHED}, và học viên vẫn
 * ghi danh được vào một khóa đã đóng. Mang theo {@link #status()} thì một loại sự kiện lo
 * được cả xuất bản, sửa nội dung lẫn lưu trữ.
 *
 * <p><b>Khi nào phát</b> (việc của course-service):
 * <ul>
 *   <li>khóa học chuyển sang {@code PUBLISHED};</li>
 *   <li>sửa một khóa học đang {@code PUBLISHED} (tên, ảnh bìa, số bài học...);</li>
 *   <li>khóa học đang {@code PUBLISHED} chuyển sang trạng thái khác.</li>
 * </ul>
 * Khóa học chưa từng xuất bản thì không phát: bản nháp không có lý do gì phải nằm trong
 * database của service khác.
 *
 * <p><b>Khóa message là {@code courseId}</b>, để mọi sự kiện của cùng một khóa học vào
 * chung một partition và tới đúng thứ tự — nếu không, bản "đã lưu trữ" có thể tới trước
 * bản "đã xuất bản" và bản sao kẹt ở trạng thái sai.
 *
 * @param status tên trạng thái trong {@code CourseStatus} của course-service:
 *               {@code DRAFT}, {@code PUBLISHED} hoặc {@code ARCHIVED}. Để dạng chuỗi vì
 *               shared-common không phụ thuộc vào enum riêng của một service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseUpdatedEvent(
        String eventId,
        Instant occurredAt,
        Long courseId,
        String title,
        String slug,
        String thumbnailUrl,
        Long instructorId,
        String instructorName,
        Integer totalLessons,
        String status
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.COURSE_UPDATED;
    }

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static CourseUpdatedEvent of(
            Long courseId, String title, String slug, String thumbnailUrl,
            Long instructorId, String instructorName, Integer totalLessons, String status) {

        return new CourseUpdatedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                courseId,
                title,
                slug,
                thumbnailUrl,
                instructorId,
                instructorName,
                totalLessons,
                status);
    }
}
