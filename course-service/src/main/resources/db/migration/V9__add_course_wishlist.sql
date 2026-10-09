-- Khóa học người dùng lưu lại để học sau; xóa khóa thì mất theo.
CREATE TABLE course_wishlist (
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, course_id),
    KEY idx_course_wishlist_course (course_id),
    CONSTRAINT fk_course_wishlist_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
