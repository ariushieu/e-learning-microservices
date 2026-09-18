-- Dữ liệu tham chiếu bắt buộc: mẫu thông báo cho các sự kiện Kafka hiện có.
INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('ENROLLMENT_SUCCESS', 'IN_APP', 'Ghi danh thành công',
        'Bạn đã ghi danh khóa học <b>{courseTitle}</b>. Bắt đầu học ngay nhé!'),
       ('ENROLLMENT_SUCCESS', 'EMAIL', 'Xác nhận ghi danh khóa học {courseTitle}',
        'Chào {fullName},\n\nBạn đã ghi danh thành công khóa học "{courseTitle}".\nTruy cập: {courseUrl}\n\nE-Learning HUNRE'),
       ('COURSE_COMPLETED', 'IN_APP', 'Chúc mừng bạn hoàn thành khóa học',
        'Bạn đã hoàn thành <b>{courseTitle}</b>. Chứng chỉ đã sẵn sàng để tải về.'),
       ('QUIZ_GRADED', 'IN_APP', 'Đã có kết quả bài kiểm tra',
        'Bài kiểm tra <b>{quizTitle}</b> của bạn đạt {score} điểm.'),
       ('CERTIFICATE_ISSUED', 'EMAIL', 'Chứng chỉ khóa học {courseTitle}',
        'Chào {fullName},\n\nChứng chỉ mã {certificateCode} đã được cấp.\nTải về: {certificateUrl}\n\nE-Learning HUNRE');
