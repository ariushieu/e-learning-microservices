-- Hỏi đáp trong bài học (sự kiện lesson.question.* của course-service).
INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('LESSON_QUESTION_POSTED', 'IN_APP', 'Học viên vừa đặt câu hỏi',
        '<b>{askerName}</b> hỏi trong bài <b>{lessonTitle}</b> ({courseTitle}): {preview}'),
       ('LESSON_QUESTION_ANSWERED', 'IN_APP', 'Câu hỏi của bạn có câu trả lời mới',
        '<b>{answererName}</b>{answererRole} trả lời trong bài <b>{lessonTitle}</b>: {preview}');
