-- Thông báo giảng viên gửi cho học viên của một khóa học; xóa khóa thì thông báo mất theo.
CREATE TABLE course_announcements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(150) NULL,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_course_announcements_course (course_id, created_at),
    CONSTRAINT fk_course_announcements_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
