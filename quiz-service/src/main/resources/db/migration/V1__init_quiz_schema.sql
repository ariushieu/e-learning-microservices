-- =============================================================================
-- quiz_db - Sở hữu bởi quiz-service
-- Bài kiểm tra, câu hỏi, phương án trả lời, lượt làm bài và kết quả chấm.
--
-- Tham chiếu xuyên service (KHÔNG có foreign key):
--   quizzes.course_id       -> course_db.courses.id
--   quizzes.lesson_id       -> course_db.lessons.id (bài học kiểu QUIZ)
--   quizzes.created_by      -> auth_db.users.id
--   quiz_attempts.user_id   -> auth_db.users.id
-- =============================================================================

-- Bài kiểm tra
CREATE TABLE quizzes
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    course_id         BIGINT        NOT NULL COMMENT 'course_db.courses.id - không đặt FK',
    lesson_id         BIGINT            NULL COMMENT 'course_db.lessons.id, null nếu là bài kiểm tra cuối khóa',
    title             VARCHAR(200)  NOT NULL,
    description       VARCHAR(1000)     NULL,
    time_limit_minutes INT              NULL COMMENT 'Null là không giới hạn thời gian',
    pass_score        DECIMAL(5, 2) NOT NULL DEFAULT 50.00 COMMENT 'Phần trăm tối thiểu để đạt',
    max_attempts      INT           NOT NULL DEFAULT 3 COMMENT '0 là không giới hạn số lần làm',
    shuffle_questions TINYINT(1)    NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_by        BIGINT        NOT NULL COMMENT 'auth_db.users.id - không đặt FK',
    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_quizzes_course (course_id, status),
    KEY idx_quizzes_lesson (lesson_id),
    CONSTRAINT ck_quizzes_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_quizzes_pass_score CHECK (pass_score BETWEEN 0 AND 100),
    CONSTRAINT ck_quizzes_max_attempts CHECK (max_attempts >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Bài kiểm tra';

-- Câu hỏi
CREATE TABLE questions
(
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    quiz_id     BIGINT        NOT NULL,
    content     TEXT          NOT NULL,
    type        VARCHAR(20)   NOT NULL DEFAULT 'SINGLE_CHOICE',
    score       DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT 'Điểm thô của câu hỏi',
    position    INT           NOT NULL DEFAULT 0,
    explanation TEXT              NULL COMMENT 'Giải thích hiển thị sau khi nộp bài',
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_questions_quiz_position (quiz_id, position),
    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT ck_questions_type CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT ck_questions_score CHECK (score > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Câu hỏi của bài kiểm tra';

-- Phương án trả lời
CREATE TABLE answer_options
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    question_id BIGINT      NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    is_correct  TINYINT(1)  NOT NULL DEFAULT 0,
    position    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_answer_options_question (question_id, position),
    CONSTRAINT fk_answer_options_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Phương án trả lời';

-- Lượt làm bài
CREATE TABLE quiz_attempts
(
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    quiz_id      BIGINT        NOT NULL,
    user_id      BIGINT        NOT NULL COMMENT 'auth_db.users.id - không đặt FK',
    attempt_no   INT           NOT NULL DEFAULT 1,
    status       VARCHAR(20)   NOT NULL DEFAULT 'IN_PROGRESS',
    score        DECIMAL(5, 2)     NULL COMMENT 'Phần trăm, chỉ có giá trị sau khi chấm',
    passed       TINYINT(1)        NULL,
    started_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_at DATETIME(6)       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quiz_attempts_quiz_user_no (quiz_id, user_id, attempt_no) COMMENT 'Chặn tạo trùng lượt làm bài',
    KEY idx_quiz_attempts_user (user_id, submitted_at),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT ck_quiz_attempts_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED')),
    CONSTRAINT ck_quiz_attempts_score CHECK (score IS NULL OR score BETWEEN 0 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Lượt làm bài kiểm tra';

-- Kết quả chấm theo từng câu hỏi trong một lượt làm bài
CREATE TABLE attempt_answers
(
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    attempt_id   BIGINT        NOT NULL,
    question_id  BIGINT        NOT NULL,
    earned_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    is_correct   TINYINT(1)    NOT NULL DEFAULT 0,
    answered_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempt_answers_attempt_question (attempt_id, question_id),
    KEY idx_attempt_answers_question (question_id),
    CONSTRAINT fk_attempt_answers_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answers_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Câu trả lời đã chấm của một lượt làm bài';

-- Các phương án người học chọn. Bảng riêng vì câu hỏi MULTIPLE_CHOICE chọn được nhiều đáp án.
CREATE TABLE attempt_answer_options
(
    attempt_answer_id BIGINT NOT NULL,
    option_id         BIGINT NOT NULL,
    PRIMARY KEY (attempt_answer_id, option_id),
    KEY idx_attempt_answer_options_option (option_id),
    CONSTRAINT fk_attempt_answer_options_answer FOREIGN KEY (attempt_answer_id) REFERENCES attempt_answers (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answer_options_option FOREIGN KEY (option_id) REFERENCES answer_options (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Phương án người học đã chọn';
