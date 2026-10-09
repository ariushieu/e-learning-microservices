package com.hunre.notificationservice.consumer;

import com.hunre.notificationservice.repository.ProcessedEventRepository;
import com.hunre.notificationservice.service.NotificationService;
import com.hunre.sharedcommon.event.CertificateIssuedEvent;
import com.hunre.sharedcommon.event.CourseAnnouncementPostedEvent;
import com.hunre.sharedcommon.event.EnrollmentCompletedEvent;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.LessonQuestionAnsweredEvent;
import com.hunre.sharedcommon.event.LessonQuestionPostedEvent;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import com.hunre.sharedcommon.event.UserProfileUpdatedEvent;
import com.hunre.sharedcommon.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Biến một sự kiện Kafka thành một thông báo, đúng một lần.
 *
 * <p>Tách khỏi {@code KafkaEventConsumer} vì cần một ranh giới transaction thật. Ghi sổ
 * {@code processed_events} và tạo thông báo phải cùng thành công hoặc cùng thất bại: ghi sổ
 * trước rồi chết giữa chừng thì sự kiện bị đánh dấu đã xử lý mà người dùng không nhận được
 * gì, và Kafka gửi lại cũng vô ích vì đã có trong sổ.
 *
 * <p>Gọi {@code @Transactional} từ một phương thức khác trong cùng lớp thì Spring không
 * chen proxy vào được, nên transaction sẽ không tồn tại. Đó là lý do lớp này là một bean
 * riêng chứ không phải một phương thức private của consumer.
 */
@Service
@RequiredArgsConstructor
public class EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * @throws org.springframework.dao.DataIntegrityViolationException khi sự kiện đã xử lý
     *         rồi; người gọi bắt lấy và bỏ qua
     */
    @Transactional
    public void process(String eventId, String eventType, String topic, String payload) {
        // Ghi sổ TRƯỚC khi xử lý, và để ràng buộc khóa chính của database quyết định.
        // Cách khác là hỏi "đã có chưa" rồi mới ghi, nhưng hai consumer chạy song song có
        // thể cùng thấy "chưa có" rồi cùng tạo thông báo — đúng cái cần tránh.
        processedEventRepository.insertNew(eventId, eventType, topic, Instant.now());

        switch (eventType) {
            case EventTypes.ENROLLMENT_CREATED -> handleEnrollmentCreated(payload);
            case EventTypes.ENROLLMENT_COMPLETED -> handleEnrollmentCompleted(payload);
            case EventTypes.QUIZ_GRADED -> handleQuizGraded(payload);
            case EventTypes.CERTIFICATE_ISSUED -> handleCertificateIssued(payload);
            case EventTypes.COURSE_ANNOUNCEMENT_POSTED -> handleAnnouncementPosted(payload);
            case EventTypes.LESSON_QUESTION_POSTED -> handleQuestionPosted(payload);
            case EventTypes.LESSON_QUESTION_ANSWERED -> handleQuestionAnswered(payload);
            case EventTypes.USER_REGISTERED -> handleUserRegistered(payload);
            case EventTypes.USER_PROFILE_UPDATED -> handleUserProfileUpdated(payload);
            // Service khác thêm loại sự kiện mới mà service này chưa biết là chuyện bình
            // thường, không phải lỗi. Vẫn ghi sổ để lần gửi lại không phải đọc lại nữa.
            default -> log.debug("Bỏ qua sự kiện loại {} vì chưa có xử lý tương ứng", eventType);
        }
    }

    /** Lưu email và tên trước, rồi mới xếp email chào mừng để thư có tên người nhận. */
    private void handleUserRegistered(String payload) {
        UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
        notificationService.saveContact(event.userId(), event.email(), event.fullName());
        notificationService.queueEmail("WELCOME", event.userId(), Map.of(), "/courses");
    }

    private void handleUserProfileUpdated(String payload) {
        UserProfileUpdatedEvent event = objectMapper.readValue(payload, UserProfileUpdatedEvent.class);
        notificationService.saveContact(event.userId(), event.email(), event.fullName());
    }

    private void handleEnrollmentCreated(String payload) {
        EnrollmentCreatedEvent event = objectMapper.readValue(payload, EnrollmentCreatedEvent.class);
        notificationService.deliver(
                "ENROLLMENT_SUCCESS",
                event.userId(),
                variables("courseTitle", event.courseTitle()),
                "/learn/" + event.courseId());
    }

    private void handleEnrollmentCompleted(String payload) {
        EnrollmentCompletedEvent event = objectMapper.readValue(payload, EnrollmentCompletedEvent.class);
        notificationService.deliver(
                "COURSE_COMPLETED",
                event.userId(),
                variables("courseTitle", event.courseTitle()),
                // enrollment-service cấp chứng chỉ trong cùng transaction với lúc hoàn thành,
                // nên khi thông báo này tới thì trang chứng chỉ đã có.
                certificatePage(event.enrollmentId()));
    }

    private void handleQuizGraded(String payload) {
        QuizGradedEvent event = objectMapper.readValue(payload, QuizGradedEvent.class);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("quizTitle", event.quizTitle());
        // toPlainString giữ nguyên 85.50 thay vì đổi thành 85.5 hay ký hiệu mũ.
        variables.put("score", event.score() == null ? "0" : event.score().toPlainString());

        notificationService.deliver(
                "QUIZ_GRADED", event.userId(), variables, "/attempts/" + event.attemptId());
    }

    private void handleCertificateIssued(String payload) {
        CertificateIssuedEvent event = objectMapper.readValue(payload, CertificateIssuedEvent.class);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("courseTitle", event.courseTitle());
        variables.put("certificateCode", event.certificateCode());
        variables.put("certificateUrl", event.certificateUrl());

        // Không dùng event.certificateUrl(): đó là đường dẫn file PDF chưa ai phục vụ, bấm vào
        // là trang lỗi. Trang chứng chỉ của web đi theo mã ghi danh.
        notificationService.deliver(
                "CERTIFICATE_ISSUED", event.userId(), variables, certificatePage(event.enrollmentId()));
    }

    /**
     * Một sự kiện, nhiều người nhận. Tất cả nằm trong một transaction với dòng ghi sổ: lỗi giữa
     * chừng thì rollback hết rồi Kafka gửi lại, không có cảnh nửa lớp nhận hai lần.
     */
    private void handleAnnouncementPosted(String payload) {
        CourseAnnouncementPostedEvent event = objectMapper.readValue(payload, CourseAnnouncementPostedEvent.class);
        if (event.recipientIds() == null || event.recipientIds().isEmpty()) {
            return;
        }

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("courseTitle", event.courseTitle());
        variables.put("announcementTitle", event.title());
        variables.put("preview", event.preview());

        String link = "/courses/" + event.courseId() + "#thong-bao";
        for (Long userId : new LinkedHashSet<>(event.recipientIds())) {
            notificationService.deliver("COURSE_ANNOUNCEMENT", userId, variables, link);
        }
    }

    private void handleQuestionPosted(String payload) {
        LessonQuestionPostedEvent event = objectMapper.readValue(payload, LessonQuestionPostedEvent.class);
        if (event.instructorId() == null) return;

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("askerName", event.askerName() == null ? "Một học viên" : event.askerName());
        variables.put("lessonTitle", event.lessonTitle());
        variables.put("courseTitle", event.courseTitle());
        variables.put("preview", event.preview());
        notificationService.deliver("LESSON_QUESTION_POSTED", event.instructorId(), variables,
                "/instructor/questions");
    }

    private void handleQuestionAnswered(String payload) {
        LessonQuestionAnsweredEvent event = objectMapper.readValue(payload, LessonQuestionAnsweredEvent.class);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("answererName", event.answererName() == null ? "Một người dùng" : event.answererName());
        variables.put("answererRole", switch (String.valueOf(event.answererRole())) {
            case "INSTRUCTOR" -> " (giảng viên)";
            case "ADMIN" -> " (quản trị viên)";
            default -> "";
        });
        variables.put("lessonTitle", event.lessonTitle());
        variables.put("preview", event.preview());
        notificationService.deliver("LESSON_QUESTION_ANSWERED", event.askerId(), variables,
                "/learn/" + event.courseId() + "?lesson=" + event.lessonId() + "#hoi-dap");
    }

    /**
     * {@code link_url} là đường dẫn trên web (frontend), luôn bắt đầu bằng "/" — frontend chỉ đi
     * theo đường dẫn nội bộ, không mở ra trang ngoài.
     */
    private static String certificatePage(Long enrollmentId) {
        return "/certificates/" + enrollmentId;
    }

    private Map<String, String> variables(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
