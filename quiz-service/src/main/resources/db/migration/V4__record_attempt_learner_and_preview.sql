-- NULL preview means a legacy attempt whose original caller roles were not recorded.
-- Do not guess whether an old administrator attempt was a learner submission.
ALTER TABLE quiz_attempts
    ADD COLUMN learner_name VARCHAR(255) NULL,
    ADD COLUMN is_preview TINYINT(1) NULL;

CREATE INDEX idx_quiz_attempts_results ON quiz_attempts (quiz_id, is_preview, status, user_id);
