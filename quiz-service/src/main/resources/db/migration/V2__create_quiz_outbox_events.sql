-- Transactional outbox: bài làm và sự kiện chấm điểm cùng commit trong quiz_db.
CREATE TABLE outbox_events
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    event_id       CHAR(36)    NOT NULL COMMENT 'UUID để consumer khử trùng lặp',
    aggregate_type VARCHAR(50) NOT NULL COMMENT 'QUIZ_ATTEMPT',
    aggregate_id   VARCHAR(50) NOT NULL,
    event_type     VARCHAR(80) NOT NULL COMMENT 'quiz.graded',
    payload        JSON        NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at   DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_events_event_id (event_id),
    KEY idx_outbox_events_unpublished (published_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Sự kiện quiz chờ gửi sang Kafka';
