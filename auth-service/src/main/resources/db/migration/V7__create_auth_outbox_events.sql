-- Transactional outbox: tài khoản và sự kiện tài khoản cùng commit trong auth_db.
CREATE TABLE outbox_events
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    event_id       CHAR(36)    NOT NULL COMMENT 'UUID để consumer khử trùng lặp',
    aggregate_type VARCHAR(50) NOT NULL COMMENT 'USER',
    aggregate_id   VARCHAR(50) NOT NULL,
    event_type     VARCHAR(80) NOT NULL COMMENT 'user.registered',
    payload        LONGTEXT    NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at   DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_events_event_id (event_id),
    KEY idx_outbox_events_unpublished (published_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Sự kiện tài khoản chờ gửi sang Kafka';

-- Nạp lần đầu: mỗi tài khoản có sẵn một sự kiện user.profile.updated để notification-service
-- dựng bảng user_contacts. Không dùng user.registered để không ai nhận lại email chào mừng.
-- Hai bước vì UUID() trong bảng dẫn xuất có thể bị tính lại ở mỗi chỗ tham chiếu.
INSERT INTO outbox_events (event_id, aggregate_type, aggregate_id, event_type, payload)
SELECT UUID(), 'USER', CAST(id AS CHAR), 'user.profile.updated', '{}'
FROM users
ORDER BY id;

-- Cùng bộ trường với UserProfileUpdatedEvent (DomainEventSerializationTest giữ hợp đồng này).
-- So sánh bằng số: aggregate_id và chuỗi CAST(... AS CHAR) khác collation, MySQL báo lỗi 1267.
UPDATE outbox_events o JOIN users u ON CAST(o.aggregate_id AS UNSIGNED) = u.id
SET o.payload = JSON_OBJECT(
        'eventId', o.event_id,
        'occurredAt', DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%dT%H:%i:%s.%fZ'),
        'userId', u.id,
        'email', u.email,
        'fullName', u.full_name,
        'eventType', 'user.profile.updated')
WHERE o.event_type = 'user.profile.updated' AND o.payload = '{}';
