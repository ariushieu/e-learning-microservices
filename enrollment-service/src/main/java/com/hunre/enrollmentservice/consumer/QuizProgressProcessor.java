package com.hunre.enrollmentservice.consumer;

import com.hunre.enrollmentservice.repository.ProcessedQuizEventRepository;
import com.hunre.enrollmentservice.service.ProgressService;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ranh giới transaction riêng: sổ sự kiện và toàn bộ thay đổi tiến độ phải cùng commit. */
@Service
@RequiredArgsConstructor
public class QuizProgressProcessor {
    private final ProcessedQuizEventRepository events;
    private final ProgressService progress;

    @Transactional
    public void process(QuizGradedEvent event) {
        try {
            events.insert(event.eventId());
        } catch (DuplicateKeyException ex) {
            // Chỉ bắt INSERT sổ sự kiện; lỗi chứng chỉ/outbox bên dưới phải rollback và báo lỗi.
            throw new DuplicateQuizEventException(ex);
        }
        if (event.passed() && event.lessonId() != null) {
            progress.completeLessonFromQuiz(event.userId(), event.courseId(), event.lessonId());
        }
    }
}
