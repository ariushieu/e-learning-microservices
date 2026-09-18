-- =============================================================================
-- course_db - Sở hữu bởi course-service
-- Danh mục, khóa học, chương (section), bài học (lesson), tài liệu, đánh giá.
--
-- Tham chiếu xuyên service (KHÔNG có foreign key, chỉ lưu id + index):
--   courses.instructor_id     -> auth_db.users.id
--   course_reviews.user_id    -> auth_db.users.id
-- =============================================================================

-- Danh mục khóa học, cho phép lồng 1 cấp cha - con
CREATE TABLE categories
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT           NULL,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(180) NOT NULL,
    description VARCHAR(500)     NULL,
    position    INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_slug (slug),
    KEY idx_categories_parent (parent_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Danh mục khóa học';

-- Khóa học
CREATE TABLE courses
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    category_id            BIGINT            NULL,
    instructor_id          BIGINT        NOT NULL COMMENT 'auth_db.users.id - không đặt FK',
    instructor_name        VARCHAR(150)      NULL COMMENT 'Bản chụp tên giảng viên để hiển thị, cập nhật qua Kafka',
    title                  VARCHAR(200)  NOT NULL,
    slug                   VARCHAR(220)  NOT NULL,
    summary                VARCHAR(500)      NULL,
    description            MEDIUMTEXT        NULL,
    thumbnail_url          VARCHAR(500)      NULL,
    level                  VARCHAR(20)   NOT NULL DEFAULT 'BEGINNER',
    language               VARCHAR(10)   NOT NULL DEFAULT 'vi',
    price                  DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'VND, 0 là miễn phí',
    status                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_lessons          INT           NOT NULL DEFAULT 0 COMMENT 'Số liệu dẫn xuất, service tự cập nhật',
    total_duration_seconds INT           NOT NULL DEFAULT 0 COMMENT 'Số liệu dẫn xuất',
    student_count          INT           NOT NULL DEFAULT 0 COMMENT 'Số liệu dẫn xuất từ sự kiện Kafka ghi danh',
    rating_avg             DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    rating_count           INT           NOT NULL DEFAULT 0,
    published_at           DATETIME(6)       NULL,
    created_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_courses_slug (slug),
    KEY idx_courses_instructor (instructor_id),
    KEY idx_courses_category_status (category_id, status),
    KEY idx_courses_status_published (status, published_at),
    CONSTRAINT fk_courses_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_courses_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_courses_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_courses_price CHECK (price >= 0),
    CONSTRAINT ck_courses_rating CHECK (rating_avg BETWEEN 0 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Khóa học';

-- Chương / phần của khóa học
CREATE TABLE sections
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    course_id  BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    position   INT          NOT NULL DEFAULT 0 COMMENT 'Thứ tự hiển thị, không đặt UNIQUE để sắp xếp lại không vướng ràng buộc',
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_sections_course_position (course_id, position),
    CONSTRAINT fk_sections_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Chương của khóa học';

-- Bài học. course_id lặp lại từ sections để truy vấn theo khóa học không phải join.
CREATE TABLE lessons
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    section_id       BIGINT       NOT NULL,
    course_id        BIGINT       NOT NULL COMMENT 'Phi chuẩn hóa có chủ đích, tránh join khi lọc theo khóa học',
    title            VARCHAR(200) NOT NULL,
    type             VARCHAR(20)  NOT NULL DEFAULT 'VIDEO',
    content_url      VARCHAR(500)     NULL COMMENT 'Link video / file với type VIDEO, FILE',
    content          MEDIUMTEXT       NULL COMMENT 'Nội dung bài đọc với type ARTICLE',
    duration_seconds INT          NOT NULL DEFAULT 0,
    position         INT          NOT NULL DEFAULT 0,
    is_preview       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Cho xem thử khi chưa ghi danh',
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_lessons_section_position (section_id, position),
    KEY idx_lessons_course (course_id),
    CONSTRAINT fk_lessons_section FOREIGN KEY (section_id) REFERENCES sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT ck_lessons_type CHECK (type IN ('VIDEO', 'ARTICLE', 'FILE', 'QUIZ'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Bài học';

-- Tài liệu đính kèm bài học
CREATE TABLE lesson_resources
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    lesson_id  BIGINT       NOT NULL,
    name       VARCHAR(200) NOT NULL,
    file_url   VARCHAR(500) NOT NULL,
    file_size  BIGINT           NULL COMMENT 'Byte',
    mime_type  VARCHAR(100)     NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_lesson_resources_lesson (lesson_id),
    CONSTRAINT fk_lesson_resources_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Tài liệu đính kèm bài học';

-- Đánh giá khóa học, mỗi người dùng đánh giá một khóa tối đa một lần
CREATE TABLE course_reviews
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    course_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL COMMENT 'auth_db.users.id - không đặt FK',
    rating     TINYINT     NOT NULL,
    comment    VARCHAR(2000)   NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_reviews_course_user (course_id, user_id),
    KEY idx_course_reviews_user (user_id),
    CONSTRAINT fk_course_reviews_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT ck_course_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Đánh giá khóa học';
