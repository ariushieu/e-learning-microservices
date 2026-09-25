package com.hunre.notificationservice.consumer;

import com.hunre.notificationservice.entity.Notification;
import com.hunre.notificationservice.entity.NotificationChannel;
import com.hunre.notificationservice.entity.NotificationPreference;
import com.hunre.notificationservice.entity.NotificationTemplate;
import com.hunre.notificationservice.repository.NotificationPreferenceRepository;
import com.hunre.notificationservice.repository.NotificationRepository;
import com.hunre.notificationservice.repository.NotificationTemplateRepository;
import com.hunre.notificationservice.repository.ProcessedEventRepository;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.KafkaTopics;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chạy trên H2 thật thay vì mock repository, vì thứ cần kiểm ở đây chính là hành vi của
 * database: ràng buộc khóa chính trên {@code processed_events} mới là cơ chế chống trùng.
 * Mock lại thì test chỉ chứng minh mock hoạt động.
 */
@SpringBootTest
class EventProcessorTest {

    @Autowired
    private EventProcessor eventProcessor;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
        templateRepository.deleteAll();
        preferenceRepository.deleteAll();

        // Flyway tắt trong test nên bảng mẫu rỗng, phải tự nạp.
        templateRepository.save(NotificationTemplate.builder()
                .code("QUIZ_GRADED")
                .channel(NotificationChannel.IN_APP)
                .titleTemplate("Đã có kết quả bài kiểm tra")
                .bodyTemplate("Bài kiểm tra {quizTitle} của bạn đạt {score} điểm.")
                .active(true)
                .build());

        templateRepository.save(NotificationTemplate.builder()
                .code("ENROLLMENT_SUCCESS")
                .channel(NotificationChannel.IN_APP)
                .titleTemplate("Ghi danh thành công")
                .bodyTemplate("Bạn đã ghi danh khóa học {courseTitle}.")
                .active(true)
                .build());
    }

    private String json(Object event) {
        return objectMapper.writeValueAsString(event);
    }

    @Test
    @DisplayName("quiz.graded tạo đúng một thông báo với nội dung đã điền sẵn")
    void taoThongBaoTuKetQuaBaiKiemTra() {
        QuizGradedEvent event = QuizGradedEvent.of(
                1L, 2L, 3L, 42L, "Chương 1: Microservices", new BigDecimal("85.50"), true);

        eventProcessor.process(event.eventId(), event.eventType(),
                KafkaTopics.QUIZ_EVENTS, json(event));

        List<Notification> all = notificationRepository.findAll();
        assertThat(all).hasSize(1);

        Notification notification = all.get(0);
        assertThat(notification.getUserId()).isEqualTo(42L);
        assertThat(notification.getType()).isEqualTo("QUIZ_GRADED");
        assertThat(notification.getTitle()).isEqualTo("Đã có kết quả bài kiểm tra");
        assertThat(notification.getContent())
                .isEqualTo("Bài kiểm tra Chương 1: Microservices của bạn đạt 85.50 điểm.");
    }

    @Test
    @DisplayName("điểm số giữ nguyên hai chữ số thập phân, không thành 85.5")
    void diemSoGiuNguyenDinhDang() {
        QuizGradedEvent event = QuizGradedEvent.of(
                1L, 2L, 3L, 42L, "Bài kiểm tra", new BigDecimal("85.50"), true);

        eventProcessor.process(event.eventId(), event.eventType(),
                KafkaTopics.QUIZ_EVENTS, json(event));

        assertThat(notificationRepository.findAll().get(0).getContent()).contains("85.50");
    }

    @Test
    @DisplayName("nhận lại cùng một sự kiện KHÔNG tạo thông báo thứ hai")
    void khongTaoTrungKhiNhanLai() {
        QuizGradedEvent event = QuizGradedEvent.of(
                1L, 2L, 3L, 42L, "Bài kiểm tra", new BigDecimal("70.00"), true);
        String payload = json(event);

        eventProcessor.process(event.eventId(), event.eventType(),
                KafkaTopics.QUIZ_EVENTS, payload);

        // Kafka bảo đảm at-least-once: consumer chết trước khi commit offset thì lần sau
        // nhận lại đúng message này. Người dùng không được thấy hai thông báo giống nhau.
        assertThatThrownBy(() -> eventProcessor.process(event.eventId(), event.eventType(),
                KafkaTopics.QUIZ_EVENTS, payload))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(notificationRepository.findAll())
                .as("lần nhận thứ hai không được sinh thêm thông báo")
                .hasSize(1);
    }

    @Test
    @DisplayName("hai sự kiện khác nhau thì tạo hai thông báo")
    void suKienKhacNhauTaoRieng() {
        QuizGradedEvent mot = QuizGradedEvent.of(
                1L, 2L, 3L, 42L, "Bài 1", new BigDecimal("60.00"), true);
        QuizGradedEvent hai = QuizGradedEvent.of(
                2L, 2L, 3L, 42L, "Bài 2", new BigDecimal("90.00"), true);

        eventProcessor.process(mot.eventId(), mot.eventType(), KafkaTopics.QUIZ_EVENTS, json(mot));
        eventProcessor.process(hai.eventId(), hai.eventType(), KafkaTopics.QUIZ_EVENTS, json(hai));

        assertThat(notificationRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("enrollment.created dùng mẫu ENROLLMENT_SUCCESS")
    void ghiDanhTaoThongBaoTuongUng() {
        EnrollmentCreatedEvent event =
                EnrollmentCreatedEvent.of(1L, 7L, 3L, "Kiến trúc Microservices");

        eventProcessor.process(event.eventId(), event.eventType(),
                KafkaTopics.ENROLLMENT_EVENTS, json(event));

        Notification notification = notificationRepository.findAll().get(0);
        assertThat(notification.getUserId()).isEqualTo(7L);
        assertThat(notification.getContent())
                .isEqualTo("Bạn đã ghi danh khóa học Kiến trúc Microservices.");
    }

    @Test
    @DisplayName("người dùng đã tắt thông báo thì không tạo, nhưng vẫn ghi sổ đã xử lý")
    void tonTrongTuyChonNguoiDung() {
        preferenceRepository.save(NotificationPreference.builder()
                .userId(42L)
                .inAppEnabled(false)
                .emailEnabled(true)
                .build());

        QuizGradedEvent event = QuizGradedEvent.of(
                1L, 2L, 3L, 42L, "Bài kiểm tra", new BigDecimal("80.00"), true);

        eventProcessor.process(event.eventId(), event.eventType(),
                KafkaTopics.QUIZ_EVENTS, json(event));

        assertThat(notificationRepository.findAll()).isEmpty();
        // Vẫn phải ghi sổ: nếu không, mỗi lần Kafka gửi lại sẽ lại đi qua toàn bộ xử lý.
        assertThat(processedEventRepository.findById(event.eventId())).isPresent();
    }

    /**
     * Dùng đúng sự kiện course-service sẽ phát: service này nghe cả topic khóa học nhưng
     * không tạo thông báo nào từ đó, nên phải bỏ qua êm chứ không được ném lỗi.
     */
    @Test
    @DisplayName("loại sự kiện chưa có xử lý vẫn được ghi sổ chứ không làm hỏng consumer")
    void suKienLaKhongLamHong() {
        String payload = """
                {"eventId":"11111111-2222-3333-4444-555555555555",
                 "eventType":"course.updated","occurredAt":"2026-09-25T03:04:05Z",
                 "courseId":9,"title":"Khóa học","slug":"khoa-hoc","thumbnailUrl":null,
                 "instructorId":7,"instructorName":null,"totalLessons":12,"status":"PUBLISHED"}
                """;

        eventProcessor.process("11111111-2222-3333-4444-555555555555",
                EventTypes.COURSE_UPDATED, KafkaTopics.COURSE_EVENTS, payload);

        assertThat(notificationRepository.findAll()).isEmpty();
        assertThat(processedEventRepository.findById("11111111-2222-3333-4444-555555555555"))
                .isPresent();
    }

    @Test
    @DisplayName("không có mẫu cho loại sự kiện thì bỏ qua, không ném lỗi")
    void thieuMauThiBoQua() {
        templateRepository.deleteAll();

        QuizGradedEvent event = QuizGradedEvent.of(
                1L, 2L, 3L, 42L, "Bài kiểm tra", new BigDecimal("80.00"), true);

        eventProcessor.process(event.eventId(), EventTypes.QUIZ_GRADED,
                KafkaTopics.QUIZ_EVENTS, json(event));

        assertThat(notificationRepository.findAll()).isEmpty();
    }
}
