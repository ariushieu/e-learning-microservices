ALTER TABLE quizzes ADD COLUMN questions_per_attempt INT NULL;
-- Retain rows referenced by already graded answers while removing them from the bank.
ALTER TABLE questions ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE quizzes ADD CONSTRAINT ck_quizzes_questions_per_attempt
    CHECK (questions_per_attempt IS NULL OR questions_per_attempt BETWEEN 1 AND 200);

-- Keep selected IDs even after author deletion: no FK to questions, no replacement draw.
CREATE TABLE attempt_questions (
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (attempt_id, question_id),
    CONSTRAINT fk_attempt_questions_attempt FOREIGN KEY (attempt_id)
        REFERENCES quiz_attempts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Existing open attempts used the full bank. Freeze it at upgrade time.
INSERT INTO attempt_questions (attempt_id, question_id)
SELECT a.id, q.id FROM quiz_attempts a JOIN questions q ON q.quiz_id = a.quiz_id
WHERE a.status = 'IN_PROGRESS';
-- Closed attempts retain only questions that were actually graded.
INSERT INTO attempt_questions (attempt_id, question_id)
SELECT aa.attempt_id, aa.question_id FROM attempt_answers aa
JOIN quiz_attempts a ON a.id = aa.attempt_id WHERE a.status <> 'IN_PROGRESS';
