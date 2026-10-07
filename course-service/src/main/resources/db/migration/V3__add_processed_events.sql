CREATE TABLE processed_events (
    event_id VARCHAR(36) NOT NULL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    source_topic VARCHAR(100) NULL,
    processed_at DATETIME(6) NOT NULL,
    INDEX idx_processed_events_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
