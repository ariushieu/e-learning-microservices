-- Bản sao email và tên người dùng, dựng từ sự kiện của auth-service (topic elearning.auth.events).
-- Sự kiện nghiệp vụ khác cố ý không mang email; muốn gửi thư thì tra bảng này.
CREATE TABLE user_contacts
(
    user_id    BIGINT       NOT NULL,
    email      VARCHAR(255) NOT NULL,
    full_name  VARCHAR(150) NOT NULL,
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'Email và tên người dùng, sao từ auth-service';

-- Hàng đợi email: dòng notifications kênh EMAIL ở trạng thái PENDING, gửi theo thứ tự id.
CREATE INDEX idx_notifications_channel_status ON notifications (channel, status, id);

-- Mẫu EMAIL ở V2 chưa từng được dùng; thống nhất đường dẫn về một biến {url}.
UPDATE notification_templates
SET body_template = 'Chào {fullName},\n\nBạn đã ghi danh thành công khóa học "{courseTitle}".\nVào học: {url}\n\nE-Learning HUNRE'
WHERE code = 'ENROLLMENT_SUCCESS' AND channel = 'EMAIL';

UPDATE notification_templates
SET body_template = 'Chào {fullName},\n\nChúc mừng bạn đã hoàn thành khóa học "{courseTitle}". Chứng chỉ mã {certificateCode} đã được cấp.\nXem và tải chứng chỉ: {url}\n\nE-Learning HUNRE'
WHERE code = 'CERTIFICATE_ISSUED' AND channel = 'EMAIL';

INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('WELCOME', 'EMAIL', 'Chào mừng bạn đến với E-Learning HUNRE',
        'Chào {fullName},\n\nTài khoản của bạn đã được tạo. Bắt đầu tìm khóa học tại: {url}\n\nE-Learning HUNRE'),
       ('COURSE_ANNOUNCEMENT', 'EMAIL', 'Thông báo mới: {announcementTitle}',
        'Chào {fullName},\n\nGiảng viên khóa "{courseTitle}" vừa gửi thông báo:\n\n{preview}\n\nĐọc đầy đủ: {url}\n\nE-Learning HUNRE'),
       ('LESSON_QUESTION_ANSWERED', 'EMAIL', 'Câu hỏi của bạn đã có trả lời',
        'Chào {fullName},\n\n{answererName}{answererRole} vừa trả lời câu hỏi của bạn ở bài "{lessonTitle}":\n\n{preview}\n\nXem trả lời: {url}\n\nE-Learning HUNRE');
