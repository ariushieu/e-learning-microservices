CREATE TABLE login_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_login_events_user_time (user_id, created_at, id),
    KEY idx_login_events_user_success (user_id, success, id),
    KEY idx_login_events_cleanup (created_at),
    CONSTRAINT fk_login_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE refresh_tokens ADD COLUMN session_started_at DATETIME(6) NULL;
-- Old rotated tokens have no family link; their creation time is the best known start.
UPDATE refresh_tokens SET session_started_at = created_at;
ALTER TABLE refresh_tokens MODIFY COLUMN session_started_at DATETIME(6) NOT NULL;
