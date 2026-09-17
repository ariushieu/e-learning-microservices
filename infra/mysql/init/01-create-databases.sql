-- Chạy tự động một lần khi container MySQL khởi tạo dữ liệu lần đầu.
-- Pattern database-per-service: mỗi service sở hữu một database riêng,
-- không service nào truy cập trực tiếp DB của service khác.

CREATE DATABASE IF NOT EXISTS auth_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS course_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS enrollment_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS quiz_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- User ứng dụng (MYSQL_USER) đã được entrypoint tạo trước khi chạy file này.
-- Cấp quyền trên cả 5 database cho user đó.
GRANT ALL PRIVILEGES ON auth_db.*         TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON course_db.*       TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON enrollment_db.*   TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON quiz_db.*         TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'elearning'@'%';
FLUSH PRIVILEGES;
