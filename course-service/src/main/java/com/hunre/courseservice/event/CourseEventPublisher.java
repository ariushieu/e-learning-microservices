package com.hunre.courseservice.event;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.OutboxEvent;
import com.hunre.courseservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.CourseAnnouncementPostedEvent;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;

/** Lưu sự kiện cùng giao dịch khóa học; worker chỉ gửi các hàng đã commit. */
@Component
@RequiredArgsConstructor
public class CourseEventPublisher {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final com.hunre.courseservice.repository.LessonRepository lessons;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishCourseUpdated(Course course) {
        if (course == null) return;
        // Ghi thay đổi và giữ khóa hàng course trước khi cấp id outbox, tránh đảo thứ tự
        // hai giao dịch đồng thời cập nhật cùng một khóa học.
        entityManager.flush();
        CourseUpdatedEvent event = CourseUpdatedEvent.of(course.getId(), course.getTitle(), course.getSlug(),
                course.getThumbnailUrl(), course.getInstructorId(), course.getInstructorName(),
                course.getTotalLessons(), course.getStatus().name(), lessons.findIdsByCourseId(course.getId()));
        // Không nuốt lỗi: nếu không lưu được sự kiện thì thay đổi khóa học cũng phải rollback.
        repository.save(OutboxEvent.builder().eventId(event.eventId()).aggregateType("COURSE")
                .aggregateId(String.valueOf(course.getId())).eventType(event.eventType())
                .payload(objectMapper.writeValueAsString(event)).build());
    }

    /** Ghi sự kiện cùng giao dịch lưu thông báo: lưu được thông báo thì chắc chắn có người nhận. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAnnouncementPosted(CourseAnnouncementPostedEvent event) {
        repository.save(OutboxEvent.builder().eventId(event.eventId()).aggregateType("COURSE")
                .aggregateId(String.valueOf(event.courseId())).eventType(event.eventType())
                .payload(objectMapper.writeValueAsString(event)).build());
    }
}
