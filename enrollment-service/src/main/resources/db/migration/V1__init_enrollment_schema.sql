-- =============================================================================
-- enrollment_db - Sở hữu bởi enrollment-service
-- Ghi danh và theo dõi tiến độ học tập, chứng chỉ và transactional outbox.
--
-- Tham chiếu xuyên service (KHÔNG có foreign key, chỉ lưu id + index):
--   enrollments.user_id            -> auth_db.users.id
--   enrollments.course_id          -> course_db.courses.id
--   lesson_progress.lesson_id      -> course_db.lessons.id
--   course_snapshots.course_id     -> course_db.courses.id
-- =============================================================================

-- Bảng ghi danh khóa học
CREATE TABLE enrollments
(
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL COMMENT 'auth_db.users.id',
    course_id        BIGINT        NOT NULL COMMENT 'course_db.courses.id',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    progress_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT 'Tiến độ từ 0.00 đến 100.00%',
    enrolled_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at     DATETIME(6)       NULL,
    last_accessed_at DATETIME(6)       NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollments_user_course (user_id, course_id),
    KEY idx_enrollments_user (user_id),
    KEY idx_enrollments_course (course_id),
    KEY idx_enrollments_status (status),
    CONSTRAINT ck_enrollments_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Bảng ghi danh khóa học của học viên';

-- Bảng tiến độ từng bài học
CREATE TABLE lesson_progress
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    enrollment_id   BIGINT      NOT NULL,
    lesson_id       BIGINT      NOT NULL COMMENT 'course_db.lessons.id',
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    watched_seconds INT         NOT NULL DEFAULT 0,
    completed_at    DATETIME(6)     NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_progress_enrollment_lesson (enrollment_id, lesson_id),
    KEY idx_lesson_progress_enrollment (enrollment_id),
    KEY idx_lesson_progress_lesson (lesson_id),
    CONSTRAINT fk_lesson_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE,
    CONSTRAINT ck_lesson_progress_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Tiến độ học tập từng bài học';

-- Bảng chứng chỉ cấp khi hoàn thành khóa học
CREATE TABLE certificates
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    enrollment_id    BIGINT       NOT NULL,
    certificate_code VARCHAR(50)  NOT NULL,
    file_url         VARCHAR(500)     NULL,
    issued_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificates_enrollment (enrollment_id),
    UNIQUE KEY uk_certificates_code (certificate_code),
    CONSTRAINT fk_certificates_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Chứng chỉ hoàn thành khóa học';

-- Bản sao chỉ đọc thông tin khóa học (đồng bộ từ course-service)
CREATE TABLE course_snapshots
(
    course_id     BIGINT       NOT NULL COMMENT 'Khóa chính, trùng với course_db.courses.id',
    title         VARCHAR(200) NOT NULL,
    slug          VARCHAR(220)     NULL,
    instructor_id BIGINT           NULL,
    total_lessons INT          NOT NULL DEFAULT 0,
    synced_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Bản sao chỉ đọc thông tin khóa học';

-- Bảng outbox events để phát sự kiện Kafka (Transactional Outbox Pattern)
CREATE TABLE outbox_events
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    event_id       CHAR(36)     NOT NULL COMMENT 'UUID định danh sự kiện',
    aggregate_type VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    published_at   DATETIME(6)      NULL COMMENT 'NULL nghĩa là chưa gửi lên Kafka broker',
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_published (published_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Transactional outbox lưu sự kiện trước khi đẩy sang Kafka';

-- View tiến độ khóa học (course_progress)
CREATE OR REPLACE VIEW course_progress AS
SELECT 
    e.id AS enrollment_id,
    e.user_id,
    e.course_id,
    e.progress_percent,
    e.status,
    e.enrolled_at,
    e.completed_at,
    e.last_accessed_at
FROM enrollments e;
