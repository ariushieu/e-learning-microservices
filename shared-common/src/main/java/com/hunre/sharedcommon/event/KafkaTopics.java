package com.hunre.sharedcommon.event;

/**
 * Tên topic Kafka của hệ thống.
 *
 * <p>Gom sự kiện theo service phát ra chứ không tạo mỗi loại sự kiện một topic. Lý do:
 * các sự kiện của cùng một service thường cần giữ đúng thứ tự với nhau (ghi danh phải
 * đến trước hoàn thành khóa học), mà Kafka chỉ bảo đảm thứ tự trong phạm vi một partition
 * của một topic. Tách ra nhiều topic là mất luôn bảo đảm đó.
 *
 * <p>Khi gửi, dùng khóa message là id của thực thể gốc (ví dụ {@code userId}) để mọi sự
 * kiện của cùng một người rơi vào cùng một partition và giữ nguyên thứ tự.
 */
public final class KafkaTopics {

    /** Sự kiện do enrollment-service phát: ghi danh, hoàn thành, cấp chứng chỉ. */
    public static final String ENROLLMENT_EVENTS = "elearning.enrollment.events";

    /** Sự kiện do course-service phát: {@link CourseUpdatedEvent}. */
    public static final String COURSE_EVENTS = "elearning.course.events";

    /** Sự kiện do quiz-service phát: chấm bài kiểm tra. */
    public static final String QUIZ_EVENTS = "elearning.quiz.events";

    private KafkaTopics() {
        throw new AssertionError("Lớp hằng số, không tạo thể hiện");
    }
}
