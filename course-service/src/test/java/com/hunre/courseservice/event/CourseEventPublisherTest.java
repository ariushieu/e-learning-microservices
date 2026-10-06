package com.hunre.courseservice.event;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private CourseEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CourseEventPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("publishCourseUpdated gửi event lên Kafka đúng topic, key là courseId và payload đầy đủ")
    void publishCourseUpdated_thanhCong() {
        Course course = Course.builder()
                .id(100L)
                .title("Khóa học Microservices")
                .slug("khoa-hoc-microservices")
                .thumbnailUrl("https://img.test/thumb.png")
                .instructorId(7L)
                .instructorName("Giảng viên A")
                .totalLessons(15)
                .status(CourseStatus.PUBLISHED)
                .build();

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishCourseUpdated(course);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopics.COURSE_EVENTS);
        assertThat(keyCaptor.getValue()).isEqualTo("100");

        CourseUpdatedEvent readEvent = objectMapper.readValue(payloadCaptor.getValue(), CourseUpdatedEvent.class);
        assertThat(readEvent.courseId()).isEqualTo(100L);
        assertThat(readEvent.title()).isEqualTo("Khóa học Microservices");
        assertThat(readEvent.slug()).isEqualTo("khoa-hoc-microservices");
        assertThat(readEvent.thumbnailUrl()).isEqualTo("https://img.test/thumb.png");
        assertThat(readEvent.instructorId()).isEqualTo(7L);
        assertThat(readEvent.instructorName()).isEqualTo("Giảng viên A");
        assertThat(readEvent.totalLessons()).isEqualTo(15);
        assertThat(readEvent.status()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("publishCourseUpdated không lỗi khi kafkaTemplate là null (môi trường tắt Kafka)")
    void publishCourseUpdated_khiKafkaTemplateNull() {
        CourseEventPublisher publisherWithoutKafka = new CourseEventPublisher(null, objectMapper);
        Course course = Course.builder().id(1L).status(CourseStatus.PUBLISHED).build();

        assertThatCode(() -> publisherWithoutKafka.publishCourseUpdated(course))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publishCourseUpdated không làm gì khi course là null")
    void publishCourseUpdated_khiCourseNull() {
        publisher.publishCourseUpdated(null);
        verifyNoInteractions(kafkaTemplate);
    }
}
