package com.hunre.courseservice.event;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hunre.courseservice.repository.OutboxEventRepository;
import com.hunre.courseservice.entity.OutboxEvent;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseEventPublisherTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private com.hunre.courseservice.repository.LessonRepository lessons;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private CourseEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CourseEventPublisher(repository, objectMapper, entityManager, lessons);
    }

    @Test
    @DisplayName("publishCourseUpdated lưu outbox với courseId và payload đầy đủ")
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

        when(lessons.findIdsByCourseId(100L)).thenReturn(java.util.List.of(1001L, 1002L));
        publisher.publishCourseUpdated(course);
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(eventCaptor.capture());
        OutboxEvent stored = eventCaptor.getValue();
        assertThat(stored.getAggregateId()).isEqualTo("100");
        assertThat(stored.getPublishedAt()).isNull();
        CourseUpdatedEvent readEvent = objectMapper.readValue(stored.getPayload(), CourseUpdatedEvent.class);
        assertThat(readEvent.courseId()).isEqualTo(100L);
        assertThat(readEvent.title()).isEqualTo("Khóa học Microservices");
        assertThat(readEvent.slug()).isEqualTo("khoa-hoc-microservices");
        assertThat(readEvent.thumbnailUrl()).isEqualTo("https://img.test/thumb.png");
        assertThat(readEvent.instructorId()).isEqualTo(7L);
        assertThat(readEvent.instructorName()).isEqualTo("Giảng viên A");
        assertThat(readEvent.totalLessons()).isEqualTo(15);
        assertThat(readEvent.status()).isEqualTo("PUBLISHED");
        assertThat(readEvent.lessonIds()).containsExactly(1001L, 1002L);
    }

    @Test
    @DisplayName("Lỗi lưu outbox phải truyền lên để rollback khóa học")
    void outboxFailureIsNotSwallowed() {
        Course course = Course.builder().id(1L).status(CourseStatus.PUBLISHED).build();
        when(repository.save(any())).thenThrow(new IllegalStateException("Database unavailable"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher.publishCourseUpdated(course))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test
    @DisplayName("publishCourseUpdated không làm gì khi course là null")
    void publishCourseUpdated_khiCourseNull() {
        publisher.publishCourseUpdated(null);
        verifyNoInteractions(repository);
    }
}
