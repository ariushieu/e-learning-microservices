package com.hunre.courseservice.consumer;

import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.ProcessedEventRepository;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

/** Ghi sổ và tăng số lượt ghi danh cùng thành công hoặc cùng rollback. */
@Service
@RequiredArgsConstructor
public class EnrollmentEventProcessor {
    private final ProcessedEventRepository processedEvents;
    private final CourseRepository courses;

    @Transactional
    public void process(EnrollmentCreatedEvent event) {
        processedEvents.insertNew(event.eventId(), event.eventType(), KafkaTopics.ENROLLMENT_EVENTS, Instant.now());
        if (courses.incrementStudentCount(event.courseId()) != 1) {
            // Giữ message trong DLT để đối soát/phát lại; không đánh dấu đã đếm.
            throw new InvalidEventException("Không tìm thấy khóa học " + event.courseId());
        }
    }
}
