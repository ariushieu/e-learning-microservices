package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.LessonAnswer;
import com.hunre.courseservice.entity.LessonQuestion;
import java.time.Instant;
import java.util.List;

/**
 * Một câu hỏi kèm mọi câu trả lời. {@code deletable} tính theo người đang xem: tác giả hoặc
 * người quản lý khóa. {@code lessonTitle}/{@code courseTitle} chỉ có trong hộp câu hỏi của giảng viên.
 */
public record LessonQuestionResponse(Long id, Long lessonId, Long courseId, String lessonTitle, String courseTitle,
                                     String authorName, String content, boolean mine, boolean deletable,
                                     boolean instructorAnswered, Instant createdAt, List<Answer> answers) {

    public record Answer(Long id, String authorName, String authorRole, String content, boolean mine,
                         boolean deletable, Instant createdAt) {
        public static Answer from(LessonAnswer a, Long viewerId, boolean manager) {
            boolean mine = a.getUserId().equals(viewerId);
            return new Answer(a.getId(), a.getAuthorName() == null ? "Người dùng" : a.getAuthorName(),
                    a.getAuthorRole().name(), a.getContent(), mine, mine || manager, a.getCreatedAt());
        }
    }

    public static LessonQuestionResponse from(LessonQuestion q, List<Answer> answers, Long viewerId, boolean manager,
                                              String lessonTitle, String courseTitle) {
        boolean mine = q.getUserId().equals(viewerId);
        return new LessonQuestionResponse(q.getId(), q.getLessonId(), q.getCourseId(), lessonTitle, courseTitle,
                q.getAuthorName() == null ? "Học viên" : q.getAuthorName(), q.getContent(), mine, mine || manager,
                q.isInstructorAnswered(), q.getCreatedAt(), answers);
    }
}
