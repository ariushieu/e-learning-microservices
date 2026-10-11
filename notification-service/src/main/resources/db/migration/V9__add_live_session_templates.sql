-- Lớp học trực tuyến (sự kiện live.session.* của course-service): lên lịch, bắt đầu, hủy.
-- Tên buổi học tối đa 150 ký tự nên tiêu đề sau khi điền vẫn dưới 255 của cột notifications.title.
INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('LIVE_SESSION_SCHEDULED', 'IN_APP', 'Lịch học trực tuyến: {sessionTitle}',
        'Khóa <b>{courseTitle}</b> có buổi học trực tuyến lúc {startsAt}, dài {durationMinutes} phút.'),
       ('LIVE_SESSION_STARTED', 'IN_APP', 'Lớp học trực tuyến đang diễn ra',
        '<b>{sessionTitle}</b> của khóa {courseTitle} đã bắt đầu. Vào lớp ngay.'),
       ('LIVE_SESSION_CANCELLED', 'IN_APP', 'Đã hủy buổi học: {sessionTitle}',
        'Buổi học trực tuyến lúc {startsAt} của khóa <b>{courseTitle}</b> đã bị hủy.'),
       ('LIVE_SESSION_SCHEDULED', 'EMAIL', 'Lịch học trực tuyến: {sessionTitle}',
        'Chào {fullName},\n\nKhóa "{courseTitle}" có buổi học trực tuyến "{sessionTitle}" lúc {startsAt}, dài {durationMinutes} phút.\n\nĐến giờ, vào lớp tại: {url}\n\nE-Learning HUNRE'),
       ('LIVE_SESSION_STARTED', 'EMAIL', 'Lớp học trực tuyến đang diễn ra: {sessionTitle}',
        'Chào {fullName},\n\nBuổi học "{sessionTitle}" của khóa "{courseTitle}" đã bắt đầu.\n\nVào lớp ngay: {url}\n\nE-Learning HUNRE'),
       ('LIVE_SESSION_CANCELLED', 'EMAIL', 'Đã hủy buổi học: {sessionTitle}',
        'Chào {fullName},\n\nBuổi học trực tuyến "{sessionTitle}" lúc {startsAt} của khóa "{courseTitle}" đã bị hủy.\n\nXem lịch học khác: {url}\n\nE-Learning HUNRE');
