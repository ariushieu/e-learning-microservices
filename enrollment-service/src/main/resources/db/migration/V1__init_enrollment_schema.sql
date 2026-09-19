-- =============================================================================
-- enrollment_db - Sở hữu bởi enrollment-service
-- Ghi danh, tiến độ học từng bài, chứng chỉ hoàn thành.
--
-- Tham chiếu xuyên service (KHÔNG có foreign key):
--   enrollments.user_id        -> auth_db.users.id
--   enrollments.course_id      -> course_db.courses.id
--   lesson_progress.lesson_id  -> course_db.lessons.id
-- =============================================================================

-- Ghi danh: một người dùng chỉ ghi danh một khóa học một lần
CREATE TABLE enrollments
(
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL COMMENT 'auth_db.users.id - không đặt FK',
    course_id        BIGINT        NOT NULL COMMENT 'course_db.courses.id - không đặt FK',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    progress_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    enrolled_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at     DATETIME(6)       NULL,
    last_accessed_at DATETIME(6)       NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollments_user_course (user_id, course_id) COMMENT 'Chặn ghi danh trùng ở tầng database',
    KEY idx_enrollments_course (course_id),
    KEY idx_enrollments_user_status (user_id, status),
    CONSTRAINT ck_enrollments_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_enrollments_progress CHECK (progress_percent BETWEEN 0 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Ghi danh khóa học';

-- Tiến độ từng bài học trong một lần ghi danh
CREATE TABLE lesson_progress
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    enrollment_id  BIGINT      NOT NULL,
    lesson_id      BIGINT      NOT NULL COMMENT 'course_db.lessons.id - không đặt FK',
    status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    watched_seconds INT        NOT NULL DEFAULT 0,
    completed_at   DATETIME(6)     NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_progress_enrollment_lesson (enrollment_id, lesson_id),
    CONSTRAINT fk_lesson_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE,
    CONSTRAINT ck_lesson_progress_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Tiến độ học từng bài';

-- Chứng chỉ, cấp một lần khi hoàn thành khóa học
CREATE TABLE certificates
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    enrollment_id     BIGINT      NOT NULL,
    certificate_code  VARCHAR(40) NOT NULL COMMENT 'Mã tra cứu công khai',
    file_url          VARCHAR(500)    NULL,
    issued_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificates_enrollment (enrollment_id),
    UNIQUE KEY uk_certificates_code (certificate_code),
    CONSTRAINT fk_certificates_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Chứng chỉ hoàn thành';

-- Bản sao chỉ đọc của khóa học, đồng bộ từ sự kiện Kafka course.*
-- Mục đích: hiển thị danh sách "khóa học của tôi" mà không gọi đồng bộ sang course-service.
CREATE TABLE course_snapshots
(
    course_id       BIGINT       NOT NULL COMMENT 'course_db.courses.id, dùng luôn làm khóa chính',
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) NOT NULL,
    thumbnail_url   VARCHAR(500)     NULL,
    instructor_id   BIGINT           NULL,
    instructor_name VARCHAR(150)     NULL,
    total_lessons   INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    synced_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Bản sao khóa học đồng bộ qua Kafka (nhất quán cuối)';

-- Transactional outbox: ghi sự kiện cùng transaction nghiệp vụ, một job đẩy sang Kafka sau.
-- Tránh trường hợp commit database xong nhưng gửi Kafka lỗi (hoặc ngược lại).
CREATE TABLE outbox_events
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    event_id       CHAR(36)    NOT NULL COMMENT 'UUID, consumer dùng để khử trùng lặp',
    aggregate_type VARCHAR(50) NOT NULL COMMENT 'ENROLLMENT, CERTIFICATE',
    aggregate_id   VARCHAR(50) NOT NULL,
    event_type     VARCHAR(80) NOT NULL COMMENT 'enrollment.created, enrollment.completed',
    payload        JSON        NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at   DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_events_event_id (event_id),
    KEY idx_outbox_events_unpublished (published_at, id) COMMENT 'Quét sự kiện chưa gửi'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Hàng đợi sự kiện chờ đẩy sang Kafka';
