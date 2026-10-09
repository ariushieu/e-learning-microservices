-- Giảng viên gửi thông báo cho cả lớp (sự kiện course.announcement.posted của course-service).
-- Tiêu đề thông báo tối đa 150 ký tự nên tiêu đề sau khi điền vẫn dưới 255 của cột notifications.title.
INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('COURSE_ANNOUNCEMENT', 'IN_APP', 'Thông báo mới: {announcementTitle}',
        'Giảng viên khóa <b>{courseTitle}</b>: {preview}');
