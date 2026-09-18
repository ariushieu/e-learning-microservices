-- =============================================================================
-- notification_db - Sở hữu bởi notification-service
-- Mẫu thông báo, thông báo đã tạo, tùy chọn nhận thông báo, sổ khử trùng lặp sự kiện.
--
-- Tham chiếu xuyên service (KHÔNG có foreign key):
--   notifications.user_id             -> auth_db.users.id
--   notification_preferences.user_id  -> auth_db.users.id
-- =============================================================================

-- Mẫu nội dung thông báo, tách khỏi code để sửa nội dung không cần build lại
CREATE TABLE notification_templates
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50) NOT NULL COMMENT 'ENROLLMENT_SUCCESS, COURSE_COMPLETED...',
    channel       VARCHAR(20) NOT NULL,
    title_template VARCHAR(255) NOT NULL COMMENT 'Có placeholder dạng {courseTitle}',
    body_template TEXT        NOT NULL,
    active        TINYINT(1)  NOT NULL DEFAULT 1,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_templates_code_channel (code, channel),
    CONSTRAINT ck_notification_templates_channel CHECK (channel IN ('IN_APP', 'EMAIL'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Mẫu thông báo';

-- Thông báo gửi tới người dùng. Nội dung được lưu sau khi đã điền placeholder,
-- để sửa template về sau không làm thay đổi thông báo đã gửi.
CREATE TABLE notifications
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT 'auth_db.users.id - không đặt FK',
    type        VARCHAR(50)  NOT NULL COMMENT 'Trùng notification_templates.code',
    channel     VARCHAR(20)  NOT NULL DEFAULT 'IN_APP',
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    link_url    VARCHAR(500)     NULL COMMENT 'Đường dẫn mở khi bấm vào thông báo',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count INT          NOT NULL DEFAULT 0,
    last_error  VARCHAR(500)     NULL,
    sent_at     DATETIME(6)      NULL,
    read_at     DATETIME(6)      NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_notifications_user_inbox (user_id, status, created_at) COMMENT 'Hộp thư của một người dùng',
    KEY idx_notifications_pending (status, created_at) COMMENT 'Job gửi lại thông báo PENDING / FAILED',
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Thông báo gửi tới người dùng';

-- Tùy chọn nhận thông báo của người dùng
CREATE TABLE notification_preferences
(
    user_id        BIGINT      NOT NULL COMMENT 'auth_db.users.id, dùng luôn làm khóa chính',
    email_enabled  TINYINT(1)  NOT NULL DEFAULT 1,
    in_app_enabled TINYINT(1)  NOT NULL DEFAULT 1,
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Tùy chọn nhận thông báo';

-- Sổ sự kiện đã xử lý (inbox pattern).
-- Kafka bảo đảm at-least-once nên một sự kiện có thể tới nhiều lần;
-- chèn event_id vào bảng này trước khi xử lý, trùng khóa thì bỏ qua.
CREATE TABLE processed_events
(
    event_id     CHAR(36)    NOT NULL COMMENT 'UUID lấy từ outbox của service phát sự kiện',
    event_type   VARCHAR(80) NOT NULL,
    source_topic VARCHAR(100)    NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id),
    KEY idx_processed_events_processed_at (processed_at) COMMENT 'Cho job dọn bản ghi cũ'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Khử trùng lặp sự kiện Kafka';
