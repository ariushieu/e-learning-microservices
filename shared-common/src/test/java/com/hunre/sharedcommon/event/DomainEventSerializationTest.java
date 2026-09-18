package com.hunre.sharedcommon.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Khóa chặt hình dạng JSON của các sự kiện Kafka.
 *
 * <p>Tên trường trong JSON chính là hợp đồng giữa service phát và service nhận. Đổi tên
 * một trường sẽ không làm hỏng biên dịch ở đâu cả, nhưng consumer sẽ đọc ra null trong
 * im lặng. Các test dưới đây tồn tại để việc đổi tên đó làm đỏ CI ngay lập tức.
 */
class DomainEventSerializationTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("EnrollmentCreatedEvent giữ nguyên bộ tên trường đã thống nhất")
    void hopDongEnrollmentCreated() {
        EnrollmentCreatedEvent event =
                EnrollmentCreatedEvent.of(1L, 2L, 3L, "Kiến trúc Microservices");

        assertThat(fieldNamesOf(event)).containsExactlyInAnyOrder(
                "eventId", "eventType", "occurredAt",
                "enrollmentId", "userId", "courseId", "courseTitle");
    }

    @Test
    @DisplayName("EnrollmentCompletedEvent giữ nguyên bộ tên trường đã thống nhất")
    void hopDongEnrollmentCompleted() {
        EnrollmentCompletedEvent event =
                EnrollmentCompletedEvent.of(1L, 2L, 3L, "Khóa học", Instant.now());

        assertThat(fieldNamesOf(event)).containsExactlyInAnyOrder(
                "eventId", "eventType", "occurredAt",
                "enrollmentId", "userId", "courseId", "courseTitle", "completedAt");
    }

    @Test
    @DisplayName("CertificateIssuedEvent giữ nguyên bộ tên trường đã thống nhất")
    void hopDongCertificateIssued() {
        CertificateIssuedEvent event = CertificateIssuedEvent.of(
                1L, 2L, 3L, 4L, "Khóa học", "CERT-2026-0001", "https://example.test/c.pdf");

        assertThat(fieldNamesOf(event)).containsExactlyInAnyOrder(
                "eventId", "eventType", "occurredAt", "certificateId", "enrollmentId",
                "userId", "courseId", "courseTitle", "certificateCode", "certificateUrl");
    }

    @Test
    @DisplayName("QuizGradedEvent giữ nguyên bộ tên trường đã thống nhất")
    void hopDongQuizGraded() {
        QuizGradedEvent event = QuizGradedEvent.of(
                1L, 2L, 3L, 4L, "Bài kiểm tra 1", new BigDecimal("85.50"), true);

        assertThat(fieldNamesOf(event)).containsExactlyInAnyOrder(
                "eventId", "eventType", "occurredAt", "attemptId", "quizId",
                "courseId", "userId", "quizTitle", "score", "passed");
    }

    @Test
    @DisplayName("eventType nằm trong JSON để consumer định tuyến được")
    void eventTypeCoTrongJson() {
        String json = mapper.writeValueAsString(
                EnrollmentCreatedEvent.of(1L, 2L, 3L, "Khóa học"));

        assertThat(mapper.readTree(json).get("eventType").asString())
                .isEqualTo("enrollment.created");
    }

    @Test
    @DisplayName("ghi ra rồi đọc lại không mất dữ liệu")
    void guiDiDocLaiKhongMatDuLieu() {
        EnrollmentCreatedEvent goc =
                EnrollmentCreatedEvent.of(10L, 20L, 30L, "Tiếng Việt có dấu");

        String json = mapper.writeValueAsString(goc);
        EnrollmentCreatedEvent docLai = mapper.readValue(json, EnrollmentCreatedEvent.class);

        assertThat(docLai).isEqualTo(goc);
    }

    @Test
    @DisplayName("điểm số giữ đúng 2 chữ số thập phân, không bị sai lệch như số thực")
    void diemSoKhongSaiLech() {
        QuizGradedEvent goc = QuizGradedEvent.of(
                1L, 2L, 3L, 4L, "Bài kiểm tra", new BigDecimal("85.50"), true);

        QuizGradedEvent docLai = mapper.readValue(
                mapper.writeValueAsString(goc), QuizGradedEvent.class);

        assertThat(docLai.score()).isEqualByComparingTo("85.50");
    }

    @Test
    @DisplayName("trường lạ trong message không làm consumer chết")
    void bqQuaTruongLa() {
        String jsonTuPhienBanMoiHon = """
                {
                  "eventId": "11111111-2222-3333-4444-555555555555",
                  "eventType": "enrollment.created",
                  "occurredAt": "2026-09-18T03:04:05Z",
                  "enrollmentId": 1,
                  "userId": 2,
                  "courseId": 3,
                  "courseTitle": "Khóa học",
                  "truongMoiThemSau": "giá trị nào đó"
                }
                """;

        EnrollmentCreatedEvent event =
                mapper.readValue(jsonTuPhienBanMoiHon, EnrollmentCreatedEvent.class);

        assertThat(event.enrollmentId()).isEqualTo(1L);
        assertThat(event.courseTitle()).isEqualTo("Khóa học");
    }

    @Test
    @DisplayName("mỗi lần tạo sự kiện sinh eventId khác nhau để khử trùng lặp hoạt động")
    void eventIdDuyNhat() {
        String a = EnrollmentCreatedEvent.of(1L, 2L, 3L, "x").eventId();
        String b = EnrollmentCreatedEvent.of(1L, 2L, 3L, "x").eventId();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("eventType của mỗi lớp khớp với hằng số trong EventTypes")
    void eventTypeKhopHangSo() {
        assertThat(EnrollmentCreatedEvent.of(1L, 1L, 1L, "x").eventType())
                .isEqualTo(EventTypes.ENROLLMENT_CREATED);
        assertThat(EnrollmentCompletedEvent.of(1L, 1L, 1L, "x", Instant.now()).eventType())
                .isEqualTo(EventTypes.ENROLLMENT_COMPLETED);
        assertThat(CertificateIssuedEvent.of(1L, 1L, 1L, 1L, "x", "c", "u").eventType())
                .isEqualTo(EventTypes.CERTIFICATE_ISSUED);
        assertThat(QuizGradedEvent.of(1L, 1L, 1L, 1L, "x", BigDecimal.ONE, true).eventType())
                .isEqualTo(EventTypes.QUIZ_GRADED);
    }

    private List<String> fieldNamesOf(DomainEvent event) {
        JsonNode node = mapper.readTree(mapper.writeValueAsString(event));
        List<String> names = new ArrayList<>();
        node.propertyNames().forEach(names::add);
        return names;
    }
}
