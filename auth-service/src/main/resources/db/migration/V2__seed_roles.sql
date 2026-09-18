-- Dữ liệu tham chiếu bắt buộc: danh mục vai trò.
-- Không phải dữ liệu demo, mọi môi trường đều cần.
INSERT INTO roles (code, name, description)
VALUES ('ROLE_STUDENT', 'Học viên', 'Ghi danh khóa học, học bài, làm bài kiểm tra'),
       ('ROLE_INSTRUCTOR', 'Giảng viên', 'Tạo và quản lý khóa học, bài học, bài kiểm tra'),
       ('ROLE_ADMIN', 'Quản trị viên', 'Quản trị toàn hệ thống, phê duyệt khóa học');
