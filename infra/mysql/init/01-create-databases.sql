-- Tạo 5 database và user ứng dụng theo pattern database-per-service:
-- mỗi service sở hữu một database riêng, không service nào truy cập trực tiếp DB của service khác.
--
-- File này chạy được ở cả hai nơi:
--   1. Tự động một lần khi container MySQL khởi tạo dữ liệu lần đầu (docker compose).
--   2. Thủ công trên MySQL cài trực tiếp trên máy, dành cho ai không dùng Docker:
--        mysql -u root -p < infra/mysql/init/01-create-databases.sql
--
-- Mọi câu lệnh đều idempotent, chạy lại nhiều lần không lỗi.

CREATE DATABASE IF NOT EXISTS auth_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS course_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS enrollment_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS quiz_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user ứng dụng nếu chưa có.
-- Với docker compose, entrypoint đã tạo user từ biến MYSQL_USER trước khi file này chạy,
-- nên câu lệnh dưới đây không làm gì và mật khẩu trong .env vẫn được giữ nguyên.
-- Với MySQL cài trực tiếp, câu lệnh này tạo user với mật khẩu dev mặc định.
-- Đây là mật khẩu môi trường phát triển, không dùng cho bất kỳ môi trường nào khác.
CREATE USER IF NOT EXISTS 'elearning'@'%' IDENTIFIED BY 'elearning';

-- Cấp quyền trên cả 5 database cho user ứng dụng.
GRANT ALL PRIVILEGES ON auth_db.*         TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON course_db.*       TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON enrollment_db.*   TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON quiz_db.*         TO 'elearning'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'elearning'@'%';
FLUSH PRIVILEGES;
