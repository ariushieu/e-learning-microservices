-- Thông báo CERTIFICATE_ISSUED cũ lưu link_url là đường dẫn file PDF (/certificates/<mã>.pdf)
-- lấy từ sự kiện. Không ai phục vụ đường dẫn đó, và trên web nó rơi vào trang chứng chỉ với
-- tham số sai. Thông báo mới trỏ tới /certificates/<mã ghi danh>; thông báo cũ không còn biết
-- mã ghi danh nên bỏ link, web sẽ mở trang Thông báo như với thông báo không có link.
UPDATE notifications
SET link_url = NULL
WHERE type = 'CERTIFICATE_ISSUED'
  AND link_url LIKE '%.pdf';

-- Bấm vào thông báo giờ mở trang chứng chỉ để xem và in, không tải file nào về.
UPDATE notification_templates
SET body_template = 'Chứng chỉ mã <b>{certificateCode}</b> cho khóa học <b>{courseTitle}</b> đã được cấp. Bấm để xem.'
WHERE code = 'CERTIFICATE_ISSUED'
  AND channel = 'IN_APP';
