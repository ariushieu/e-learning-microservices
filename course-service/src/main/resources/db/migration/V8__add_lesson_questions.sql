-- Hỏi đáp trong bài học: câu hỏi của học viên và các câu trả lời, xóa bài học thì mất theo.
CREATE TABLE lesson_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    author_name VARCHAR(150) NULL,
    content VARCHAR(2000) NOT NULL,
    answer_count INT NOT NULL DEFAULT 0,
    instructor_answered BOOLEAN NOT NULL DEFAULT FALSE,
    last_activity_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lesson_questions_lesson (lesson_id, last_activity_at),
    KEY idx_lesson_questions_course (course_id, instructor_answered, last_activity_at),
    CONSTRAINT fk_lesson_questions_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE lesson_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    author_name VARCHAR(150) NULL,
    author_role VARCHAR(20) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lesson_answers_question (question_id, created_at),
    CONSTRAINT fk_lesson_answers_question FOREIGN KEY (question_id) REFERENCES lesson_questions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
