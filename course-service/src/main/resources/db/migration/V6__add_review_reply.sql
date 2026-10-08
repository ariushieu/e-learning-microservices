-- Một phản hồi của người quản lý gắn với đánh giá; xóa đánh giá thì phản hồi mất theo.
ALTER TABLE course_reviews
    ADD COLUMN reply VARCHAR(1000) NULL,
    ADD COLUMN replied_at DATETIME(6) NULL,
    ADD COLUMN replied_by BIGINT NULL;
