-- Lưu tên hiển thị tại lần viết đánh giá; không công khai email người học.
ALTER TABLE course_reviews ADD COLUMN author_name VARCHAR(150) NULL;
