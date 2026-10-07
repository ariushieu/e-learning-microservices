-- Đếm số người từng ghi danh, không đếm số lần tạo sự kiện ghi danh.
CREATE TABLE course_learners (
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, user_id),
    CONSTRAINT fk_course_learners_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
