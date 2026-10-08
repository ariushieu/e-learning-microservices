-- null phân biệt snapshot cũ chưa có đề cương với đề cương rỗng [].
ALTER TABLE course_snapshots ADD COLUMN lesson_ids JSON NULL;

-- Ghi cùng transaction với tiến độ, chứng chỉ và outbox; rollback thì Kafka được thử lại.
CREATE TABLE processed_quiz_events
(
    event_id     VARCHAR(36) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin;
