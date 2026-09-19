package com.hunre.notificationservice.consumer;

import com.hunre.notificationservice.repository.ProcessedEventRepository;
import com.hunre.notificationservice.service.NotificationService;
import com.hunre.sharedcommon.event.CertificateIssuedEvent;
import com.hunre.sharedcommon.event.EnrollmentCompletedEvent;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
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
            // Service khác thêm loại sự kiện mới mà service này chưa biết là chuyện bình
            // thường, không phải lỗi. Vẫn ghi sổ để lần gửi lại không phải đọc lại nữa.
            default -> log.debug("Bỏ qua sự kiện loại {} vì chưa có xử lý tương ứng", eventType);
        }
    }

    private void handleEnrollmentCreated(String payload) {
        EnrollmentCreatedEvent event = objectMapper.readValue(payload, EnrollmentCreatedEvent.class);
        notificationService.createInApp(
                "ENROLLMENT_SUCCESS",
                event.userId(),
                variables("courseTitle", event.courseTitle()),
                null);
    }

    private void handleEnrollmentCompleted(String payload) {
        EnrollmentCompletedEvent event = objectMapper.readValue(payload, EnrollmentCompletedEvent.class);
        notificationService.createInApp(
                "COURSE_COMPLETED",
                event.userId(),
                variables("courseTitle", event.courseTitle()),
                null);
    }

    private void handleQuizGraded(String payload) {
        QuizGradedEvent event = objectMapper.readValue(payload, QuizGradedEvent.class);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("quizTitle", event.quizTitle());
        // toPlainString giữ nguyên 85.50 thay vì đổi thành 85.5 hay ký hiệu mũ.
        variables.put("score", event.score() == null ? "0" : event.score().toPlainString());

        notificationService.createInApp("QUIZ_GRADED", event.userId(), variables, null);
    }

    private void handleCertificateIssued(String payload) {
        CertificateIssuedEvent event = objectMapper.readValue(payload, CertificateIssuedEvent.class);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("courseTitle", event.courseTitle());
        variables.put("certificateCode", event.certificateCode());
        variables.put("certificateUrl", event.certificateUrl());

        notificationService.createInApp(
                "CERTIFICATE_ISSUED", event.userId(), variables, event.certificateUrl());
    }

    private Map<String, String> variables(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
