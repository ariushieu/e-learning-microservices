-- Buổi học trực tuyến của một khóa: giảng viên lên lịch, đến giờ mở phòng, học viên vào học
-- bằng camera/micro ngay trên web. Hình và tiếng đi thẳng giữa các trình duyệt (WebRTC); máy chủ
-- chỉ giữ lịch, điểm danh và chuyển tin báo hiệu.
CREATE TABLE live_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL,
    host_name VARCHAR(150) NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NULL,
    scheduled_at DATETIME(6) NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NULL,
    ended_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_live_sessions_course (course_id, scheduled_at),
    CONSTRAINT fk_live_sessions_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Điểm danh: mỗi người một hàng mỗi buổi. Vào ra nhiều lần thì cộng dồn thời gian có mặt.
CREATE TABLE live_session_attendance (
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(150) NULL,
    host BOOLEAN NOT NULL DEFAULT FALSE,
    first_joined_at DATETIME(6) NOT NULL,
    last_left_at DATETIME(6) NULL,
    join_count INT NOT NULL DEFAULT 1,
    attended_seconds BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (session_id, user_id),
    CONSTRAINT fk_live_attendance_session FOREIGN KEY (session_id)
        REFERENCES live_sessions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
