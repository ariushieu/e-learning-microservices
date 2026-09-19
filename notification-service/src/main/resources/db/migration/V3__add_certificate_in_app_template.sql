-- =============================================================================
-- V2 chỉ nạp mẫu EMAIL cho CERTIFICATE_ISSUED, không có mẫu IN_APP.
-- Hệ quả: học viên được cấp chứng chỉ nhưng không thấy thông báo nào trong ứng dụng,
-- vì hiện chưa có máy chủ mail nên kênh EMAIL chưa gửi được gì.
--
-- Thêm bằng file mới chứ không sửa V2: V2 đã chạy trên máy mọi người, sửa lại sẽ làm
-- Flyway báo sai checksum và service không khởi động. Xem CONTRIBUTING.md mục 6.
-- =============================================================================

INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('CERTIFICATE_ISSUED', 'IN_APP', 'Chứng chỉ của bạn đã sẵn sàng',
        'Chứng chỉ mã <b>{certificateCode}</b> cho khóa học <b>{courseTitle}</b> đã được cấp. Bấm để tải về.');
