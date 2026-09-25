package com.hunre.sharedcommon.event;

/**
 * Danh mục loại sự kiện. Giá trị ở đây được ghi vào cột {@code outbox_events.event_type}
 * và dùng để định tuyến ở phía consumer.
 *
 * <p>Luôn dùng hằng số trong lớp này thay vì gõ chuỗi trực tiếp: gõ tay thì một bên viết
 * {@code "enrollment.created"} còn bên kia viết {@code "enrollmentCreated"} là consumer
 * không bao giờ nhận được message, mà chẳng có lỗi nào hiện ra.
 */
public final class EventTypes {

    /** Người học vừa ghi danh một khóa học. Phát bởi enrollment-service. */
    public static final String ENROLLMENT_CREATED = "enrollment.created";

    /** Người học hoàn thành toàn bộ khóa học. Phát bởi enrollment-service. */
    public static final String ENROLLMENT_COMPLETED = "enrollment.completed";

    /** Chứng chỉ vừa được cấp. Phát bởi enrollment-service. */
    public static final String CERTIFICATE_ISSUED = "certificate.issued";

    /** Một lượt làm bài kiểm tra đã được chấm xong. Phát bởi quiz-service. */
    public static final String QUIZ_GRADED = "quiz.graded";

    /**
     * Ảnh chụp trạng thái hiện tại của một khóa học đã từng xuất bản. Phát bởi course-service.
     * Xem {@link CourseUpdatedEvent} về lý do không tách riêng {@code course.published}.
     */
    public static final String COURSE_UPDATED = "course.updated";

    private EventTypes() {
        throw new AssertionError("Lớp hằng số, không tạo thể hiện");
    }
}
