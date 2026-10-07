-- Snapshot thông tin hiển thị; chứng chỉ cũ được bổ sung khi chủ sở hữu mở lại.
-- Giữ chứng chỉ cũ: thông tin còn thiếu được chụp khi chính chủ mở lại chứng chỉ.
-- Chứng chỉ mới lưu cả hai trường ngay trong transaction cấp chứng chỉ.
ALTER TABLE certificates
    ADD COLUMN learner_name VARCHAR(150) NULL,
    ADD COLUMN course_title VARCHAR(200) NULL;
